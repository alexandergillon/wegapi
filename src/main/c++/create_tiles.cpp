#include <Windows.h>
#include <string>
#include <stdexcept>
#include <iostream>
#include <filesystem>
#include <pathcch.h>
#include <shlwapi.h>
#include <shlobj.h>
#include <stdlib.h>

#include "constants.h"
#include "util.h"
#include "ico_parser.h"

// create_tiles.exe %cd% 0:black-king-cream,1:black-king-olive
// create_tiles.exe %cd% 0:black-king-cream && ie4uinit.exe -show
// C:\Users\alexa\OneDrive\Misc\Projects\wegapi\src\main\c++\bin\Debug 0:black-king-cream,1:black-king-olive

// ie4uinit.exe -show

// todo: / vs \ in paths
// todo: check more syscalls
// todo: wstring everywhere?
// todo: memory leaks not handled as program will shortly exit
// todo: more than max_dir

namespace {
    enum Mode {
        WEGAPI_CREATE,
        WEGAPI_CREATE_NEW,
        WEGAPI_OVERWRITE_EXISTING
    };
    const DWORD ICONDIR_RESOURCE_NUMBER = 1;
    wchar_t *game_dir_global = NULL; // for notify_explorer(), which is an atexit() handler
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
            L"Default behavior (when no option is supplied) is to overwrite existing tiles, or create them if they do not exist.\n"
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
[[maybe_unused]] static void print_args(wchar_t *game_dir, std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>>& parsed_data, Mode mode) {
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
    wchar_t *wcstok_context;

    wchar_t *index_wchar = wcstok_s(token, L":", &wcstok_context);
    if (index_wchar == NULL) {
        parse_error(L"Index of some tile is not present.");
    }
    std::wstring index_wstring(index_wchar);
    int32_t index;
    try {
        index = std::stoi(index_wstring);
    } catch ([[maybe_unused]] std::invalid_argument const& ex) {
        parse_error(L"Index " + index_wstring + L" is not valid.");
    } catch ([[maybe_unused]] std::out_of_range const& ex) {
        parse_error(L"Index " + index_wstring + L" is out of range.");
    }

    wchar_t *icon_name = wcstok_s(NULL, L":", &wcstok_context);
    if (icon_name == NULL) {
        parse_error(L"Icon name of some tile is not present.");
    }
    std::wstring icon_name_wstring(icon_name);

    // If null, no tile name. We are ok to pass this directly to the map, as NULL there indicates no tile name.
    wchar_t *tile_name = wcstok_s(NULL, L":", &wcstok_context);
    if (tile_name != NULL) {
        wchar_t *fourth_component = wcstok_s(NULL, L":", &wcstok_context);
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
 * tile to be created, and tile_name is the name of the tile to be created. tile_name may be NULL, in which case the
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

/**
 * Validates that a filename could be a .ico file. This is when it has the .ico extension, or no extension. \n \n
 *
 * Returns the filename with .ico removed, if it was present. \n \n
 *
 * On error, prints a message and exits.
 *
 * @param icon_name the filename to validate
 * @return the input with .ico removed, if it was present.
 */
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
        wegapi::util::check_success(hr, L"PathCchRemoveExtension");
        exit(EXIT_FAILURE);
    }

    return icon_name_wchar;
}

/**
 * Get the path for an icon, from its name and the game directory. Icons are stored in .\\.gamedata\\resources. \n \n
 *
 * On error, prints an error message and exits.
 *
 * @param game_dir the game directory
 * @param icon_name the name of the icon
 * @return the path of the icon, as a NULL-terminated wide character string
 */
static wchar_t *get_icon_path(wchar_t *game_dir, wchar_t *icon_name) {
    wchar_t *icon_path = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    wcscpy_s(icon_path, 1+_MAX_PATH, game_dir);

    HRESULT hr = PathCchAppend(icon_path, 1+_MAX_PATH, L"\\.gamedata\\resources\\");
    if (!wegapi::util::check_success(hr, L"PathCchAppend")) {
        exit(EXIT_FAILURE);
    }

    hr = PathCchAppend(icon_path, 1+_MAX_PATH, icon_name);
    if (!wegapi::util::check_success(hr, L"PathCchAppend")) {
        exit(EXIT_FAILURE);
    }

    // PathCchAppend adds an extra slash, which doesn't work. e.g /.gamedata/resources/name/.ico
    wcscat_s(icon_path, 1+_MAX_PATH, L".ico");

    return icon_path;
}

/**
 * Get the path of a tile, from the game directory, its index, and it's name. \n \n
 *
 * On error, prints an error message and exits.
 *
 * @param game_dir the game directory
 * @param index the index of the tile to get the path of
 * @param tile_visible_name the name that the player of the game will see on the tile (can be NULL, in which case the
 *                          tile has no name)
 * @return the path of the tile
 */
static wchar_t *get_tile_path(wchar_t *game_dir, int32_t index, [[maybe_unused]] wchar_t *tile_visible_name) {
    // todo: incorporate visible name
    wchar_t *tile_path = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    wcscpy_s(tile_path, 1+_MAX_PATH, game_dir);

    wchar_t *tile_name = wegapi::filenames::index_to_filename_with_exe(index);
    HRESULT hr = PathCchAppend(tile_path, 1+_MAX_PATH, tile_name);

    if (!wegapi::util::check_success(hr, L"PathCchAppend")) {
        exit(EXIT_FAILURE);
    }

    return tile_path;
}

/**
 * Get the base tile path, which is the tile that is copied to create new tiles. \n \n
 *
 * On error, prints an error message and exits.
 *
 * @param game_dir the game directory
 * @return the path of the base tile
 */
static wchar_t *get_base_tile_path(wchar_t *game_dir) {
    wchar_t *base_tile_path = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    wcscpy_s(base_tile_path, 1+_MAX_PATH, game_dir);

    HRESULT hr = PathCchAppend(base_tile_path, 1+_MAX_PATH, L"\\.gamedata\\tile.exe");
    if (!wegapi::util::check_success(hr, L"PathCchAppend")) {
        exit(EXIT_FAILURE);
    }

    return base_tile_path;
}

/**
 * Copies a tile from one location to another. On failure, prints an error message and exits.
 *
 * @param from path of the file to be copied
 * @param to path of the new copy to be made
 */
static void copy_exit_on_failure(wchar_t *from, wchar_t *to) {
    if (!CopyFileW(from, to, FALSE)) {
        wegapi::util::print_last_error((L"CopyFileW, " + std::wstring(from) + L" --> " + std::wstring(to)).c_str());
        exit(EXIT_FAILURE);
    }
}

/**
 * Enforces that a tile creation agrees with the specified mode, potentially creating a new tile file if needed. \n \n
 *
 * If mode is CREATE, then creates a tile if it doesn't exist. \n
 * If mode is CREATE_NEW, then creates a tile if it doesn't exist. If it does, prints an error message and exits. \n
 * If mode is OVERWRITE_EXISTING, then checks that the tile exists. If not, prints an error message and exits.
 *
 * @param tile_path path to check/create
 * @param mode mode (how to handle existing tiles, etc.)
 * @param base_tile_path path to the base tile, to be used if creating new tiles
 */
static void enforce_mode(wchar_t *tile_path, Mode mode, wchar_t *base_tile_path) {
    switch (mode) {
        case WEGAPI_CREATE:
            if (wegapi::util::path_exists(tile_path)) {
                return;
            } else {
                copy_exit_on_failure(base_tile_path, tile_path);
                return;
            }
        case WEGAPI_CREATE_NEW:
            if (wegapi::util::path_exists(tile_path)) {
                std::wcout << L"Error: CREATE_NEW was specified, but " << std::wstring(tile_path) << L" already exists." << std::endl;
                wegapi::util::wait_for_user();
                exit(EXIT_FAILURE);
            } else {
                copy_exit_on_failure(base_tile_path, tile_path);
                return;
            }
        case WEGAPI_OVERWRITE_EXISTING:
            if (!wegapi::util::path_exists(tile_path)) {
                std::wcout << L"Error: OVERWRITE_EXISTING was specified, but " << std::wstring(tile_path) << L" doesn't exist." << std::endl;
                wegapi::util::wait_for_user();
                exit(EXIT_FAILURE);
            } else {
                return;
            }
    }
}

/**
 * Creates a tile with a specified index, icon and name.
 *
 * @param game_dir the game directory
 * @param index index of the tile to create
 * @param name name of the tile to create
 * @param icon_resource_data data describing the icon of the tile, as parsed by wegapi::icons::ico_to_icon_resource
 * @param mode mode (how to handle existing tiles, etc.)
 * @param base_tile_path path to the base tile, to be used if creating new tiles
 */
static void create_tile(wchar_t *game_dir, int index, wchar_t *name, wegapi::icons::RT_GROUP_ICON_DATA icon_resource_data, Mode mode, wchar_t *base_tile_path) {
    wchar_t *tile_path = get_tile_path(game_dir, index, name);

    enforce_mode(tile_path, mode, base_tile_path);

    HANDLE exe = BeginUpdateResourceW(tile_path, FALSE); // does not need to be closed by CloseHandle()
    if (exe == NULL) {
        wegapi::util::print_last_error((std::wstring(L"create_tile, BeginUpdateResourceW, ") + std::wstring(tile_path)).c_str()); // ugly
        return;
    }

    // update the icon directory in the executable
    if (!UpdateResourceW(exe, RT_GROUP_ICON, MAKEINTRESOURCEW(ICONDIR_RESOURCE_NUMBER),
                         MAKELANGID(LANG_NEUTRAL, SUBLANG_NEUTRAL), icon_resource_data.header, icon_resource_data.header_size)) {
        wegapi::util::print_last_error(L"create_tile, UpdateResourceW header");
        return;
    }

    // then, add every image to it also
    for (wegapi::icons::RT_ICON_DATA image : icon_resource_data.images) {
        if (!UpdateResourceW(exe, RT_ICON, MAKEINTRESOURCEW(image.resource_number),
                             MAKELANGID(LANG_NEUTRAL, SUBLANG_NEUTRAL), image.data, image.size)) {
            wegapi::util::print_last_error(L"create_tile, UpdateResourceW image");
            return;
        }
    }

    if (!EndUpdateResource(exe, FALSE)) {
        wegapi::util::print_last_error(L"create_tile, EndUpdateResource");
        return;
    }
}

/**
 * Creates a number of tiles, all with the same specified icon. \n \n
 *
 * On error, prints a message and exits.
 *
 * @param game_dir the game directory
 * @param icon_name the name of the icon, in the /.gamedata/resources directory
 * @param tiles the tiles to create, as pairs (index, name)
 * @param mode mode (how to handle existing tiles, etc.)
 * @param base_tile_path path to the base tile, to be used if creating new tiles
 */
static void create_tiles_with_icon(wchar_t *game_dir, std::wstring icon_name, std::vector<std::pair<int32_t, wchar_t*>>& tiles, Mode mode, wchar_t *base_tile_path) {
    wchar_t *icon_name_wchar = validate_icon_name(icon_name);
    wchar_t *icon_path = get_icon_path(game_dir, icon_name_wchar);

    wegapi::icons::RT_GROUP_ICON_DATA icon_resource_data = wegapi::icons::ico_to_icon_resource(icon_path);

    for (auto& [index, name] : tiles) {
        create_tile(game_dir, index, name, icon_resource_data, mode, base_tile_path);
    }
}

/**
 * Creates tiles from the data passed in to the program at the command line. \n \n
 *
 * On error, prints a message and exits.
 *
 * @param game_dir the game directory
 * @param data the data, representing the tiles to create
 * @param mode mode (how to handle existing tiles, etc.)
 */
static void create_tiles(wchar_t *game_dir, std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>>& data, Mode mode) {
    wchar_t *base_tile_path = NULL;
    switch (mode) {
        case WEGAPI_CREATE:
        case WEGAPI_CREATE_NEW:
            base_tile_path = get_base_tile_path(game_dir);
            break;
        case WEGAPI_OVERWRITE_EXISTING:
            break;
    }

    for (auto& [icon_name, tiles] : data) {
        create_tiles_with_icon(game_dir, icon_name, tiles, mode, base_tile_path);
    }
}

/**
 * Tells Windows Explorer that the game directory changed, which prompts it to invalidate the icon cache for tiles
 * that changed, and redraw any new icons. I.e. refreshes Windows Explorer for the player. This function is registered
 * to run at exit with atexit(), so that if the program terminates at any point any already completed changes are
 * shown. E.g. if the third tile causes an exit, the first two tiles which have already been changed are shown to the user.
 */
static void notify_explorer() {
    SHChangeNotify(SHCNE_UPDATEDIR, SHCNF_PATH, game_dir_global, NULL);
}

/**
 * Main function. Creates tiles based on command-line arguments.
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

    if (!wegapi::util::check_exists_perror(game_dir, L"create_tiles: game directory doesn't exist")) {
        exit(EXIT_FAILURE);
    }

    std::unordered_map<std::wstring, std::vector<std::pair<int32_t, wchar_t*>>> parsed_data = parse_data(data);

    Mode mode = parse_option(option);

    //print_args(game_dir, parsed_data, mode);

    // installs exit handler to refresh Windows Explorer on exit, so that even on abnormal exit updates are shown to the player
    size_t game_dir_len = 1+wcslen(game_dir);
    game_dir_global = (wchar_t*)malloc(sizeof(wchar_t) * game_dir_len);
    wcscpy_s(game_dir_global, game_dir_len, game_dir);
    atexit(notify_explorer);

    create_tiles(game_dir, parsed_data, mode);

    exit(EXIT_SUCCESS);
}