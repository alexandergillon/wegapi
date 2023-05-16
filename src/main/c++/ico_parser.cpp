#include <string>
#include <iostream>
#include <vector>
#include <unordered_map>

#include "ico_parser.h"
#include "helpers.h"

typedef struct {
    BYTE        bWidth;          // Width, in pixels, of the image
    BYTE        bHeight;         // Height, in pixels, of the image
    BYTE        bColorCount;     // Number of colors in image (0 if >=8bpp)
    BYTE        bReserved;       // Reserved ( must be 0)
    WORD        wPlanes;         // Color Planes
    WORD        wBitCount;       // Bits per pixel
    DWORD       dwBytesInRes;    // How many bytes in this resource?
    DWORD       dwImageOffset;   // Where in the file is this image?
} ICONDIRENTRY, *LPICONDIRENTRY;

#pragma pack(push)
#pragma pack(2)
typedef struct {
    WORD           idReserved;   // Reserved (must be 0)
    WORD           idType;       // Resource Type (1 for icons)
    WORD           idCount;      // How many images?
    ICONDIRENTRY   idEntries[1]; // An entry for each image (idCount of 'em)
} ICONDIR, *LPICONDIR;
#pragma pack(pop)

// #pragmas are used here to ensure that the structure's
// packing in memory matches the packing of the EXE or DLL.
#pragma pack( push )
#pragma pack( 2 )
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
} GRPICONDIRENTRY, *LPGRPICONDIRENTRY;
#pragma pack( pop )

#pragma pack( push )
#pragma pack( 2 )
typedef struct {
    WORD              idReserved;   // Reserved (must be 0)
    WORD              idType;       // Resource type (1 for icons)
    WORD              idCount;      // How many images?
    GRPICONDIRENTRY   idEntries[1]; // The entries for each image
} GRPICONDIR, *LPGRPICONDIR;
#pragma pack( pop )

namespace {
    class ParseException : public std::exception {
        public:
            ParseException(std::wstring error_message) {
                this->error_message = error_message;
            }

            const char *what() {
                return "ParseException. Error message is a wide string: use e.what_wchar() or e.what_wstring() instead of e.what().";
            }

            const wchar_t *what_wchar() {
                return error_message.c_str();
            }

            const std::wstring what_wstring() {
                return error_message;
            }
        private:
            std::wstring error_message;
    };
}

static void *memory_map_icon(wchar_t *icon_path, HANDLE& icon, HANDLE& icon_file_mapped, DWORD& icon_size) {
    icon = CreateFileW(icon_path, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (icon == INVALID_HANDLE_VALUE) {
        wegapi::print_last_error((std::wstring(L"memory_map_icon, CreateFileW on ") + std::wstring(icon_path)).c_str()); // ugly
        exit(EXIT_FAILURE);
    }

    DWORD file_size_high;
    DWORD file_size_low = GetFileSize(icon, &file_size_high);
    // if either of file_size_high/file_size_low are non-zero, (file_size_high | file_size_low) will be non-zero
    if ((file_size_high | file_size_low) == 0) {
        // file is empty
        std::wcout << L"Error: icon " << std::wstring(icon_path) << L" is empty." << std::endl;
        wegapi::wait_for_user();
        CloseHandle(icon);
        exit(EXIT_FAILURE);
    } else if (file_size_high != 0) {
        // file is too large
        std::wcout << L"Error: icon " << std::wstring(icon_path) << L" is too large." << std::endl;
        wegapi::wait_for_user();
        CloseHandle(icon);
        exit(EXIT_FAILURE);
    }
    icon_size = file_size_low;

    icon_file_mapped = CreateFileMappingW(icon, NULL, PAGE_READONLY, 0, 0, NULL);
    if (icon_file_mapped == NULL) {
        wegapi::print_last_error(L"memory_map_icon, CreateFileMappingW");
        CloseHandle(icon);
        exit(EXIT_FAILURE);
    }

    void *icon_memory_mapping = MapViewOfFile(icon_file_mapped, FILE_MAP_READ, 0, 0, 0);
    if (icon_memory_mapping == NULL) {
        wegapi::print_last_error(L"memory_map_icon, MapViewOfFile");
        CloseHandle(icon);
        CloseHandle(icon_file_mapped);
        exit(EXIT_FAILURE);
    }

    return icon_memory_mapping;
}

wegapi::icons::RT_GROUP_ICON_DATA parse_and_convert(void *icon_memory_mapping, wchar_t *icon_path) {
    using namespace wegapi::icons;

    ICONDIR *icon_dir = (ICONDIR*)icon_memory_mapping;

    if (icon_dir->idType != 1) {
        throw ParseException(L"Error: icon " + std::wstring(icon_path) + L" is not an icon (type != 1).");
    }

    WORD num_images = icon_dir->idCount;

    if (num_images == 0) {
        throw ParseException(L"Error: icon " + std::wstring(icon_path) + L" contains no icons (size = 0).");
    }

    GRPICONDIR grpicondir;
    grpicondir.idReserved = 0;
    grpicondir.idType = 1;
    grpicondir.idCount = num_images;

    std::vector<GRPICONDIRENTRY> grpicondirentries;
    std::vector<RT_ICON_DATA> images;

    for (WORD i = 0; i < num_images; i++) {
        ICONDIRENTRY *icondirentry = &(icon_dir->idEntries[i]);
        GRPICONDIRENTRY grpicondirentry;

        grpicondirentry.bWidth = icondirentry->bWidth;
        grpicondirentry.bHeight = icondirentry->bHeight;
        grpicondirentry.bColorCount = icondirentry->bColorCount;
        grpicondirentry.bReserved = icondirentry->bReserved;
        grpicondirentry.wPlanes = icondirentry->wPlanes;
        grpicondirentry.wBitCount = icondirentry->wBitCount;
        grpicondirentry.dwBytesInRes = icondirentry->dwBytesInRes;
        grpicondirentry.nID = i + 1; // +1 because the GRPICONDIR will have id 0

        grpicondirentries.push_back(grpicondirentry);

        BYTE *data_start = ((BYTE*)icon_memory_mapping) + icondirentry->dwImageOffset;
        RT_ICON_DATA icon_data;
        icon_data.size = icondirentry->dwBytesInRes;
        icon_data.data = (BYTE*)malloc(icon_data.size);
        icon_data.resource_number = grpicondirentry.nID;
        memcpy(icon_data.data, data_start, icondirentry->dwBytesInRes);

        images.push_back(icon_data);
    }

    grpicondir.idEntries[0] = grpicondirentries[0];
    DWORD header_size = (DWORD)(sizeof(GRPICONDIR) + (sizeof(GRPICONDIRENTRY) * (grpicondirentries.size() - 1))); // -1 because the first one is part of GRPICONDIR
    BYTE *header = (BYTE*)malloc(header_size);

    memcpy(header, &grpicondir, sizeof(GRPICONDIR));
    memcpy(header + sizeof(GRPICONDIR), (&grpicondirentries.data()[1]), sizeof(GRPICONDIRENTRY) * (grpicondirentries.size() - 1));

    RT_GROUP_ICON_DATA result;
    result.header = header;
    result.header_size = header_size;
    result.images = std::move(images);

    return result;
}

wegapi::icons::RT_GROUP_ICON_DATA wegapi::icons::ico_to_icon_resource(wchar_t* icon_path) {
    HANDLE icon;  // must be closed
    HANDLE icon_file_mapped; // must be closed
    DWORD icon_size; // for now, unused - perhaps bounds check?
    void *icon_memory_mapping = memory_map_icon(icon_path, icon, icon_file_mapped, icon_size); // must be unmapped

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
        exit(EXIT_FAILURE);
    }

    return result;
}