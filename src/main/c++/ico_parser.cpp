#include <string>
#include <iostream>
#include <vector>

#include "ico_parser.h"
#include "helpers.h"

/**
 * Structure of an ICONDIRENTRY on disk, in a .ico file. An ICONDIRENTRY stores metadata about one of the images in
 * a .ico file. \n \n
 *
 * See https://learn.microsoft.com/en-us/previous-versions/ms997538(v=msdn.10) for more information about icons:
 * this struct definition was taken from there.
 */
typedef struct {
    BYTE        bWidth;          // Width, in pixels, of the image
    BYTE        bHeight;         // Height, in pixels, of the image
    BYTE        bColorCount;     // Number of colors in image (0 if >=8bpp)
    BYTE        bReserved;       // Reserved ( must be 0)
    WORD        wPlanes;         // Color Planes
    WORD        wBitCount;       // Bits per pixel
    DWORD       dwBytesInRes;    // How many bytes in this resource?
    DWORD       dwImageOffset;   // Where in the file is this image?
} ICONDIRENTRY;

/**
 * Structure of an ICONDIR on disk, in a .ico file. An ICONDIR is the header of a .ico file: it contains metadata
 * such as the number of images in the file. \n \n
 *
 * See https://learn.microsoft.com/en-us/previous-versions/ms997538(v=msdn.10) for more information about icons:
 * this struct definition was taken from there. That source has no pragma pack, but testing reveals that .ico indeed
 * has no padding for this struct, so the pragma is required. \n \n
 *
 * The pragma packs are necessary, as the .ico file format has no padding.
 * Todo: potentially refactor to not use these structs, and instead read/write bytes directly. They have unaligned
 * memory access, which likely degrades performance and may not be safe on all systems.
 */
#pragma pack(push)
#pragma pack(2) // necessary, see above
typedef struct {
    WORD           idReserved;   // Reserved (must be 0)
    WORD           idType;       // Resource Type (1 for icons)
    WORD           idCount;      // How many images?
    ICONDIRENTRY   idEntries[1]; // An entry for each image (idCount of 'em) - length of this 'array' is actually equal
                                 // to idCount
} ICONDIR;
#pragma pack(pop)

/**
 * Structure of an GRPICONDIRENTRY on disk, in a PE (portable executable) file. A GRPICONDIRENTRY stores metadata about
 * an RT_ICON resource, which is one of the images in a RT_GROUP_ICON resource. \n \n
 *
 * See https://learn.microsoft.com/en-us/previous-versions/ms997538(v=msdn.10) for more information about icons:
 * this struct definition was taken from there. \n \n
 *
 * The pragma packs are necessary, as the resource format has no padding.
 * Todo: potentially refactor to not use these structs, and instead read/write bytes directly. They have unaligned
 * memory access, which likely degrades performance and may not be safe on all systems.
 */
#pragma pack(push)
#pragma pack(2) // necessary, see above
typedef struct
{
    BYTE    bWidth;               // Width, in pixels, of the image
    BYTE    bHeight;              // Height, in pixels, of the image
    BYTE    bColorCount;          // Number of colors in image (0 if >=8bpp)
    BYTE    bReserved;            // Reserved
    WORD    wPlanes;              // Color Planes
    WORD    wBitCount;            // Bits per pixel
    DWORD   dwBytesInRes;         // how many bytes in this resource?
    WORD    nID;                  // the ID
} GRPICONDIRENTRY;
#pragma pack(pop)

/**
 * Structure of an GRPICONDIR on disk, in a PE (portable executable) file. A GRPICONDIR stores metadata about
 * a RT_GROUP_ICON resource, such as which RT_ICONs are contained within it. \n \n
 *
 * See https://learn.microsoft.com/en-us/previous-versions/ms997538(v=msdn.10) for more information about icons:
 * this struct definition was taken from there. \n \n
 *
 * The pragma packs are necessary, as the resource format has no padding.
 * Todo: potentially refactor to not use these structs, and instead read/write bytes directly. They have unaligned
 * memory access, which likely degrades performance and may not be safe on all systems.
 */
#pragma pack(push)
#pragma pack(2) // necessary, see above
typedef struct {
    WORD              idReserved;   // Reserved (must be 0)
    WORD              idType;       // Resource type (1 for icons)
    WORD              idCount;      // How many images?
    GRPICONDIRENTRY   idEntries[1]; // The entries for each image - length of this 'array' is actually equal to
                                    // idCount
} GRPICONDIR;
#pragma pack(pop)

namespace { // anonymous namespace for internal linkage
    /**
     * Exception for if parsing a .ico file fails, usually due to an invalid/corrupt file.
     */
    class ParseException : public std::exception {
        public:
            /**
             * ParseException constructor. Takes an error message associated with the exception.
             *
             * @param error_message error message, returned when calling the what_wchar() and what_wstring() methods
             */
            ParseException(std::wstring error_message) {
                this->error_message = error_message;
            }

            /**
             * Returns a generic error message associated with a ParseException. Use what_wchar() or what_wstring() instead.
             *
             * @return a generic error message
             */
            const char *what() {
                return "ParseException. Error message is a wide string: use e.what_wchar() or e.what_wstring() instead of e.what().";
            }

            /**
             * Returns the error message that this exception was constructed with, as a C-style NULL-terminated wide character string.
             *
             * @return an error message for this exception, as a C-style NULL-terminated wide character string
             */
            const wchar_t *what_wchar() {
                return error_message.c_str();
            }

            /**
             * Returns the error message that this exception was constructed with, as a C++ std::wstring
             *
             * @return an error message for this exception, as a C++ std::wstring
             */
            const std::wstring what_wstring() {
                return error_message;
            }
        private:
            std::wstring error_message;
    };
}

/**
 * Memory maps an icon_ref (.ico file).\n \n
 *
 * On error (file does not exist, is too large, Windows API failure), prints an error message and exits. \n \n
 *
 * Does not support icons larger than 2^32 bytes in size. In practice, this should not be a limitation.
 *
 * @param icon_path Path to the icon_ref
 * @param icon[out] Reference to a HANDLE. On return, this will contain a handle to the icon_ref file on disk, which must
 *                  be closed by CloseHandle()
 * @param icon_file_mapped Reference to a HANDLE. On return, this will contain a handle to a file mapping on the icon_ref
 *                         file, which must be closed by CloseHandle()
 * @param icon_size Reference to a DWORD. On return, the DWORD will contain the size of the icon_ref file, in bytes.
 * @return
 */
static void *memory_map_icon(wchar_t *icon_path, HANDLE *icon, HANDLE *icon_file_mapped, DWORD *icon_size) {
    // I think refs make cleaner code, but wanted the function signature to be C-style to match other code
    HANDLE& icon_ref = *icon;
    HANDLE& icon_file_mapped_ref = *icon_file_mapped;
    DWORD& icon_size_ref = *icon_size;

    // open file for read access
    icon_ref = CreateFileW(icon_path, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (icon_ref == INVALID_HANDLE_VALUE) {
        wegapi::print_last_error((std::wstring(L"memory_map_icon, could not open file ") + std::wstring(icon_path)).c_str());
        exit(EXIT_FAILURE);
    }

    DWORD file_size_high;
    DWORD file_size_low = GetFileSize(icon_ref, &file_size_high);
    /* if either of file_size_high/file_size_low are non-zero, (file_size_high | file_size_low) will be non-zero
       hence if file is empty, (file_size_high | file_size_low) will be zero */
    if ((file_size_high | file_size_low) == 0) {
        // file is empty
        std::wcout << L"Error: icon_ref " << std::wstring(icon_path) << L" is empty." << std::endl;
        wegapi::wait_for_user();
        CloseHandle(icon_ref);
        exit(EXIT_FAILURE);
    } else if (file_size_high != 0) {
        // file is too large (> 2^32 bytes)
        std::wcout << L"Error: icon_ref " << std::wstring(icon_path) << L" is too large." << std::endl;
        wegapi::wait_for_user();
        CloseHandle(icon_ref);
        exit(EXIT_FAILURE);
    }
    icon_size_ref = file_size_low;

    // map the file
    icon_file_mapped_ref = CreateFileMappingW(icon_ref, NULL, PAGE_READONLY, 0, 0, NULL);
    if (icon_file_mapped_ref == NULL) {
        wegapi::print_last_error(L"memory_map_icon, CreateFileMappingW");
        CloseHandle(icon_ref);
        exit(EXIT_FAILURE);
    }

    // get a buffer with the file in it
    void *icon_memory_mapping = MapViewOfFile(icon_file_mapped_ref, FILE_MAP_READ, 0, 0, 0);
    if (icon_memory_mapping == NULL) {
        wegapi::print_last_error(L"memory_map_icon, MapViewOfFile");
        CloseHandle(icon_ref);
        CloseHandle(icon_file_mapped_ref);
        exit(EXIT_FAILURE);
    }

    return icon_memory_mapping;
}

/**
 * Parses a memory-mapped .ico file, and converts it into the format required by a RT_GROUP_ICON and associated RT_ICON
 * resources in a PE (Portable Executable) file. \n \n
 *
 * See ico_parser.h for more information on the structure of the return value. \n \n
 *
 * On error, throws a ParseException.
 *
 * @param icon_memory_mapping a .ico file, memory-mapped
 * @param icon_path the path of the .ico file, for error messages
 *
 * @return a RT_GROUP_ICON_DATA, which contains all the information required to change the icon of an executable, via
 *         UpdateResource() - see ico_parser.h for more info
 */
wegapi::icons::RT_GROUP_ICON_DATA parse_and_convert(void *icon_memory_mapping, wchar_t *icon_path) {
    using namespace wegapi::icons;

    ICONDIR *icon_dir = (ICONDIR*)icon_memory_mapping; // todo: avoid structures and unaligned reads

    if (icon_dir->idType != 1) {
        // type must be 1 for icons, as specified by .ico file format
        throw ParseException(L"Error: icon " + std::wstring(icon_path) + L" is not an icon (type != 1).");
    }

    WORD num_images = icon_dir->idCount;
    if (num_images == 0) {
        throw ParseException(L"Error: icon " + std::wstring(icon_path) + L" contains no icons (size = 0).");
    }

    GRPICONDIR grpicondir; // header for PE icon resource
    grpicondir.idReserved = 0; // must be 0
    grpicondir.idType = 1; // 1 for icon
    grpicondir.idCount = num_images;

    std::vector<GRPICONDIRENTRY> grpicondirentries; // entries for each image in the resource
    std::vector<RT_ICON_DATA> images; // image binary data

    for (WORD i = 0; i < num_images; i++) {
        ICONDIRENTRY *icondirentry = &(icon_dir->idEntries[i]);
        GRPICONDIRENTRY grpicondirentry;

        // copy metadata
        grpicondirentry.bWidth = icondirentry->bWidth;
        grpicondirentry.bHeight = icondirentry->bHeight;
        grpicondirentry.bColorCount = icondirentry->bColorCount;
        grpicondirentry.bReserved = icondirentry->bReserved;
        grpicondirentry.wPlanes = icondirentry->wPlanes;
        grpicondirentry.wBitCount = icondirentry->wBitCount;
        grpicondirentry.dwBytesInRes = icondirentry->dwBytesInRes;
        grpicondirentry.nID = i + 1; // +1 because the GRPICONDIR will have id 0

        // copy image binary data
        RT_ICON_DATA icon_data;
        icon_data.size = icondirentry->dwBytesInRes;
        icon_data.data = (BYTE*)malloc(icon_data.size);
        icon_data.resource_number = grpicondirentry.nID;

        BYTE *data_start = ((BYTE*)icon_memory_mapping) + icondirentry->dwImageOffset;
        memcpy(icon_data.data, data_start, icondirentry->dwBytesInRes);

        grpicondirentries.push_back(grpicondirentry);
        images.push_back(icon_data);
    }

    /* the GRPICONDIR struct contains a 'dummy' array of length 1 to demonstrate the binary format required by a PE file.
     * So that we can copy the entire struct into the binary buffer we are building, we put the first (index 0)
     * GRPICONDIRENTRY into this array, and copy the rest (indices 1 through n-1) into the buffer afterwards. */
    grpicondir.idEntries[0] = grpicondirentries[0];

    size_t header_size_size_t = sizeof(GRPICONDIR) + (sizeof(GRPICONDIRENTRY) * (grpicondirentries.size() - 1)); // -1 because the first one is part of grpicondir
    DWORD header_size = (DWORD)header_size_size_t;
    BYTE *header = (BYTE*)malloc(header_size);

    memcpy(header, &grpicondir, sizeof(GRPICONDIR));
    memcpy(header + sizeof(GRPICONDIR), (&grpicondirentries.data()[1]), // [1] because the first one is part of grpicondir
           sizeof(GRPICONDIRENTRY) * (grpicondirentries.size() - 1)); // -1 because the first one is part of grpicondir

    RT_GROUP_ICON_DATA result;
    result.header = header;
    result.header_size = header_size;
    result.images = std::move(images);

    return result;
}

/**
 * Parses a .ico file, and converts it into the format required by a RT_GROUP_ICON and associated RT_ICON
 * resources in a PE (Portable Executable) file. \n \n
 *
 * See ico_parser.h for more information on the structure of the return value. \n \n
 *
 * On error, prints a message and exits.
 *
 * @param icon_memory_mapping a .ico file, memory-mapped
 * @param icon_path the path of the .ico file, for error messages
 *
 * @return a RT_GROUP_ICON_DATA, which contains all the information required to change the icon of an executable, via
 *         UpdateResource() - see ico_parser.h for more info
 */
wegapi::icons::RT_GROUP_ICON_DATA wegapi::icons::ico_to_icon_resource(wchar_t* icon_path) {
    HANDLE icon;  // must be closed
    HANDLE icon_file_mapped; // must be closed
    DWORD icon_size; // for now, unused. todo: perhaps bounds check to protect against bad .ico files?
    void *icon_memory_mapping = memory_map_icon(icon_path, &icon, &icon_file_mapped, &icon_size); // must be unmapped

    wegapi::icons::RT_GROUP_ICON_DATA result;
    bool failed = false;
    try {
        result = parse_and_convert(icon_memory_mapping, icon_path);
    } catch (ParseException e) {
        std::wcout << e.what_wstring() << std::endl;
        wegapi::wait_for_user();
        failed = true;
    }

    UnmapViewOfFile(icon_memory_mapping);
    CloseHandle(icon_file_mapped);
    CloseHandle(icon);

    if (failed) {
        // if we failed, parse_and_convert already printed a message
        exit(EXIT_FAILURE);
    }

    return result;
}