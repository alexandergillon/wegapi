#include <Windows.h>
#include <string>
#include <stdexcept>
#include <iostream>
#include <filesystem>
#include <pathcch.h>
#include <shlwapi.h>

#include "constants.h"
#include "helpers.h"

// devenv /debugexe create_tiles.exe %cd% 0:black-king-cream,1:black-king-olive
// C:\Users\alexa\OneDrive\Misc\Projects\wegapi\src\main\c++\bin\Debug 0:black-king-cream,1:black-king-olive

// todo: / vs \ in paths
// todo: check more syscalls

namespace {
    enum Mode {
        WEGAPI_CREATE,
        WEGAPI_CREATE_NEW,
        WEGAPI_OVERWRITE_EXISTING
    };
}

/**
 * Prints an error message, followed by a help message, and then exits.
 *
 * @param error_message error message to print
 */
[[noreturn]] static void parse_error(std::wstring error_message) {
    std::wstring help_message =
            L"usage: create_tiles.exe <DIR> <DATA> [OPT]\n"
            L"  <DIR>         Directory to create tiles in.\n"
            L"  <DATA>        Data about which tiles to create, as a comma-separated string of tiles.\n"
            L"  <OPT>         An option: one of -n or -o\n"
            L"\n"
            L"Options:\n"
            L"  -n        create only if tile doesn't already exist - throws an error if tile exists\n"
            L"  -o        create only if tile already exists (i.e., overwrite) - throws an error if tile doesn't exist\n"
            L"\n"
            L"A tile is one of the following:\n"
            L"  - A comma-delimited pair of an index (describing the index of the tile to be created), and a file "
                L"name (describing the icon of the new tile, which must be a .ico file in .gamedata/resources).\n"
            L"\n"
            L"  - A comma-delimited triple of an index (describing the index of the tile to be created), a file "
                L"name (describing the icon of the new tile, which must be a .ico file in .gamedata/resources), "
                L"and a string (which will be the text of the tile).\n"
            L"\n"
            L"Icon names may or may not contain the .ico extension - both are acceptable.\n"
            L"\n"
            L"For example:\n"
            L"  0:path0:name0,1:path1:name1,2:path2,3:path3\n"
            L"\n"
            L"It is crucial that paths/names with whitespace in <DATA> are appropriately handled, usually by wrapping "
            L"them like \"this\". However, for convenience, as long as the entire <DATA> string contains no \" "
            L"characters, it is sufficient to instead wrap the entire <DATA> string (i.e. \"<DATA>\" and not "
            L"individual paths. For example, instead of:\n"
            L"  0:\"path with a space\":\"name with a space\",1:\"another path w/ space\",\"another name w/ space\"\n"
            L"\n"
            L"We could instead use:\n"
            L"  \"0:path with a space:name with a space,1:another path w/ space,another name w/ space\"\n";

    std::wcout << error_message << L"\n";
    std::wcout << help_message << std::endl;

    exit(EXIT_FAILURE);
}

/**
 * Converts a Mode to a string.
 *
 * @param mode mode to convert
 * @return a string representation of that mode
 */
static std::wstring mode_to_string(Mode mode) {
    switch (mode) {
        case WEGAPI_CREATE:
            return L"CREATE";
        case WEGAPI_CREATE_NEW:
            return L"CREATE_NEW";
        case WEGAPI_OVERWRITE_EXISTING:
            return L"OVERWRITE_EXISTING";
        default:
            parse_error(L"Unrecognized mode value.");
    }
}

/**
 * Prints the parsed arguments of the program.
 *
 * @param game_dir the game directory string, from argv
 * @param parsed_data map representing parsed tile data
 * @param mode parsed mode
 */
static void print_args(wchar_t *game_dir, std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>>& parsed_data, Mode mode) {
    using namespace std;
    wcout << L"dir: " << wstring(game_dir) << L"\n";
    wcout << L"map: " << L"\n";
    for (auto& [icon_name, tiles] : parsed_data) {
        wcout << L"\t" << icon_name << ": [";
        for (auto pair : tiles) {
            wcout << L"(" << pair.first << ", " << (pair.second == NULL ? L"NULL" : std::wstring(pair.second)) << L"), ";
        }
        wcout << L"]" << "\n";
    }
    wcout << mode_to_string(mode) << endl;
}

/**
 * Parses a tile 'token', and adds it to the output map. \n \n
 *
 * A tile 'token' is one of the following: \n
 *   - index:icon_name \n
 *   - index:icon_name:file_name \n \n
 *
 * Where: \n
 *   - index: the index of the tile to create \n
 *   - icon_name: name of the .ico file that will be the icon of the tile \n
 *   - file_name: name of the tile \n \n
 *
 * See parse_error's help message for more information and examples. \n \n
 *
 * On error (invalid token), prints an error message and exits.
 */
static void parse_token(std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>>& out, wchar_t *token) {
    wchar_t *wcstok_buffer;

    wchar_t *index_input = wcstok_s(token, L":", &wcstok_buffer);
    if (index_input == NULL) {
        parse_error(L"Index of some tile is not present.");
    }
    std::wstring index_wstring(index_input);
    int32_t index;
    try {
        index = std::stoi(index_wstring);
    } catch ([[maybe_unused]] std::invalid_argument const& ex) {
        parse_error(L"Index " + index_wstring + L" is not valid.");
    } catch ([[maybe_unused]] std::out_of_range const& ex) {
        parse_error(L"Index " + index_wstring + L" is out of range.");
    }

    wchar_t *icon_name = wcstok_s(NULL, L":", &wcstok_buffer);
    if (icon_name == NULL) {
        parse_error(L"Icon name of some tile is not present.");
    }
    std::wstring icon_name_wstring(icon_name);

    // If null, no tile name. We are ok to pass this directly to the map, as NULL there indicates no tile name.
    wchar_t *tile_name = wcstok_s(NULL, L":", &wcstok_buffer);
    if (tile_name != NULL) {
        wchar_t *fourth_component = wcstok_s(NULL, L":", &wcstok_buffer);
        if (fourth_component != NULL) {
            parse_error(L"Unrecognized 4th component of tile: " + std::wstring(fourth_component));
        }
    }

    // If icon_name_wstring is not in the map, C++ automatically creates an empty vector for it in the map when we query it
    out[icon_name_wstring].push_back(std::pair<int32_t, wchar_t*>(index, tile_name));
}

/**
 * Parses the data argument of the program, returning a map that contains the tiles that need to be created. \n \n
 *
 * The map returned is a map from icon file names to a pair (index, tile_name), where index is the index of the
 * tile to be created, and tile_name is the name of the tile to be created. tile_name may be null, in which case the
 * tile will be created without a name. \n \n
 *
 * Input data is given in the form of comma-separated 'tiles'. See parse_error's help message for more information
 * and examples. \n \n
 *
 * On error (invalid input data), prints an error message and exits.
 */
static std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>> parse_data(wchar_t *data) {
    std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>> parsed_data;

    wchar_t *wcstok_buffer;
    for (wchar_t *token = wcstok_s(data, L",", &wcstok_buffer);
            token != NULL;
            token = wcstok_s(NULL, L",", &wcstok_buffer)) {
        parse_token(parsed_data, token);
    }

    if (parsed_data.empty()) {
        parse_error(L"No data supplied.");
    }

    return parsed_data;
}

/**
 * Parses an option. If the input string is NULL (i.e. no option flag was provided), returns the default option of
 * CREATE. If the option is -n, returns CREATE_NEW. If the option is -o, returns OVERWRITE_EXISTING. If the option
 * has any other value, prints an error message and exits.
 *
 * @param option the option to parse
 * @return an option, specified by the argument
 */
static Mode parse_option(wchar_t *option) {
    if (option == NULL) {
        return WEGAPI_CREATE;
    } else if (wcsncmp(option, L"-n", 3) == 0) { // compare 1 extra char so we don't get false positives
        return WEGAPI_CREATE_NEW;
    } else if (wcsncmp(option, L"-o", 3) == 0) { // compare 1 extra char so we don't get false positives
        return WEGAPI_OVERWRITE_EXISTING;
    } else {
        parse_error(L"Unrecognized option.");
    }
}

static wchar_t *validate_icon_name(std::wstring icon_name) {
    size_t icon_name_wchar_size = 1 + icon_name.size();
    wchar_t *icon_name_wchar = (wchar_t*)malloc(sizeof(wchar_t) * icon_name_wchar_size);
    wcscpy_s(icon_name_wchar, icon_name_wchar_size, icon_name.c_str());

    wchar_t *extension = PathFindExtensionW(icon_name_wchar);
    if (*extension != L'\0') {
        // name has an extension
        if (wcsncmp(extension, L".ico", 5) != 0) { // compare 1 extra char so we don't get false positives
            parse_error(L"Icon name " + icon_name + L" has an extension other than .ico");
        }
    }

    HRESULT hr = PathCchRemoveExtension(icon_name_wchar, icon_name_wchar_size);
    if (!(hr == S_OK || hr == S_FALSE)) {
        wegapi::check_success(hr, L"PathCchRemoveExtension");
        exit(EXIT_FAILURE);
    }

    return icon_name_wchar;
}

static wchar_t *get_icon_path(wchar_t *game_dir, wchar_t *icon_name) {
    wchar_t *icon_path = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    wcscpy_s(icon_path, 1+_MAX_PATH, game_dir);

    HRESULT hr = PathCchAppend(icon_path, 1+_MAX_PATH, L"\\.gamedata\\resources\\");
    if (!wegapi::check_success(hr, L"PathCchAppend")) {
        exit(EXIT_FAILURE);
    }

    hr = PathCchAppend(icon_path, 1+_MAX_PATH, icon_name);
    if (!wegapi::check_success(hr, L"PathCchAppend")) {
        exit(EXIT_FAILURE);
    }

    wcscat_s(icon_path, 1+_MAX_PATH, L".ico");

    return icon_path;
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

static wchar_t *get_tile_path(wchar_t *game_dir, int32_t index, [[maybe_unused]] wchar_t *tile_visible_name) {
    // todo: incorporate visible name
    wchar_t *tile_path = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    wcscpy_s(tile_path, 1+_MAX_PATH, game_dir);

    wchar_t *tile_name = wegapi::filenames::index_to_filename_exe(index);
    HRESULT hr = PathCchAppend(tile_path, 1+_MAX_PATH, tile_name);

    if (!wegapi::check_success(hr, L"PathCchAppend")) {
        exit(EXIT_FAILURE);
    }

    std::wcout << L"tile_path: " << std::wstring(tile_path) << std::endl;

    return tile_path;
}

static void create_tiles_with_icon(wchar_t *game_dir, std::wstring icon_name, std::vector<std::pair<int32_t, wchar_t*>>& tiles) {
    wchar_t *icon_name_wchar = validate_icon_name(icon_name);
    wchar_t *icon_path = get_icon_path(game_dir, icon_name_wchar);

    HANDLE icon;  // must be closed
    HANDLE icon_file_mapped; // must be closed
    DWORD icon_size;
    void *icon_memory_mapping = memory_map_icon(icon_path, icon, icon_file_mapped, icon_size); // must be unmapped

    for (auto& [index, name] : tiles) {
        wchar_t *tile_path = get_tile_path(game_dir, index, name);
        HANDLE resource = BeginUpdateResourceW(tile_path, FALSE);
        if (resource == NULL) {
            wegapi::print_last_error((std::wstring(L"create_tiles_with_icon, BeginUpdateResourceW, ") + std::wstring(tile_path)).c_str()); // ugly
            continue;
        }

        if (!UpdateResourceW(resource, RT_GROUP_ICON, RT_GROUP_ICON, MAKELANGID(LANG_NEUTRAL, SUBLANG_NEUTRAL), icon_memory_mapping, icon_size)) {
            wegapi::print_last_error(L"create_tiles_with_icon, UpdateResourceW");
            CloseHandle(resource);
            continue;
        }

        if (!EndUpdateResource(resource, FALSE)) {
            wegapi::print_last_error(L"create_tiles_with_icon, EndUpdateResource");
            CloseHandle(resource);
            continue;
        }

        CloseHandle(resource);
    }

    UnmapViewOfFile(icon_memory_mapping);
    CloseHandle(icon_file_mapped);
    CloseHandle(icon);
}

static void create_tiles(wchar_t *game_dir, std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>>& data, [[maybe_unused]] Mode mode) {
    // todo: incorporate mode
    for (auto& [icon_name, tiles] : data) {
        create_tiles_with_icon(game_dir, icon_name, tiles);
    }
}

/**
 * Main function. Creates tiles based on command-line arguments (TODO: not yet implemented, only arg parsing for now).
 *
 * @param argc number of arguments
 * @param argv[1] the directory to create the tiles in
 * @param argv[2] data which specifies the tiles to create (see parse_error's help message for more information and examples)
 * @param argv[3] optional: a mode option (either -n or -o). -n specifies that tiles should only be created if they don't exist.
 *                -o specifies tiles should only be created if they already exist.
 */
int wmain(int argc, wchar_t* argv[]) {
    wchar_t *game_dir = argv[1];
    wchar_t *data = argv[2];
    wchar_t *option = NULL;

    if (argc != 3 && argc != 4) {
        parse_error(L"Incorrect number of arguments.");
    } else if (argc == 4) {
        option = argv[3];
    }

    if (!wegapi::check_exists(game_dir, L"create_tiles: game directory doesn't exist")) {
        exit(EXIT_FAILURE);
    }

    std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>> parsed_data = parse_data(data);

    Mode mode = parse_option(option);

    print_args(game_dir, parsed_data, mode);
    create_tiles(game_dir, parsed_data, mode);
    exit(EXIT_SUCCESS);
}