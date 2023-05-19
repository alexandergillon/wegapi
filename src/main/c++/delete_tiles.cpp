#include <Windows.h>
#include <string>
#include <stdexcept>
#include <iostream>
#include <filesystem>
#include <pathcch.h>
#include <shlwapi.h>
#include <shlobj.h>
#include <stdlib.h>
#include <unordered_set>

#include "constants.h"
#include "util.h"
#include "ico_parser.h"

// .\.gamedata\delete_tiles.exe %cd% 0,1 -e
// .\.gamedata\create_tiles.exe %cd% 0:black-king-cream,1:black-king-olive,2:black-king-cream


// todo: / vs \ in paths
// todo: check more syscalls
// todo: wstring everywhere?
// todo: memory leaks not handled as program will shortly exit
// todo: more than max_dir

namespace {
    enum Mode {
        WEGAPI_DELETE,
        WEGAPI_DELETE_EXISTING,
        WEGAPI_DELETE_ALL
    };
    wchar_t *game_dir_global = NULL; // for notify_explorer(), which is an atexit() handler

    const int NUM_DELETE_RETRIES = 10;
    const DWORD DELETE_RETRY_DELAY = 50; // ms
}

/**
 * Prints an error message, followed by a help message, and then exits.
 *
 * @param error_message error message to print
 */
[[noreturn]] static void parse_error(std::wstring error_message) {
    std::wstring help_message =
            L"usage: delete_tiles.exe <DIR> <DATA> [OPT]\n"
            L"  <DIR>         Directory to delete tiles in.\n"
            L"  <DATA>        Data about which tiles to delete, as a comma-separated string of tile indices.\n"
            L"  <OPT>         An option: one of -e or -a\n"
            L"\n"
            L"Options:\n"
            L"  -e        delete only if tile already exists - throws an error if tile doesn't exist\n"
            L"  -a        deletes all tiles - in this case, data is ignored, but some data must be present (it is suggested to pass 0 as the data string)\n"
            L"Default behavior (when no option is supplied) is to delete tiles whether or not they exist.\n"
            L"\n"
            L"Again, note that if the -a flag is passed, it must be preceded by some data, even though it will be ignored.\n";

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
        case WEGAPI_DELETE:
            return L"DELETE";
        case WEGAPI_DELETE_EXISTING:
            return L"DELETE_EXISTING";
        case WEGAPI_DELETE_ALL:
            return L"DELETE_ALL";
        default:
            parse_error(L"Unrecognized mode value.");
    }
}

/**
 * Prints the parsed arguments of the program.
 *
 * @param game_dir the game directory string, from argv
 * @param tiles_to_delete set of tile indices to be deleted
 * @param mode parsed mode
 */
[[maybe_unused]] static void print_args(wchar_t *game_dir, std::unordered_set<int32_t> tiles_to_delete, Mode mode) {
    using namespace std;
    wcout << L"dir: " << wstring(game_dir) << L"\n";
    wcout << L"indices: " << L"\n";
    for (auto& tile_index : tiles_to_delete) {
        wcout << L"\t" << tile_index << ", ";
    }
    wcout << "\n";
    wcout << mode_to_string(mode) << endl;
}

/**
 * Parses the data argument of the program, returning a set that contains the indices of the tiles to be deleted. \n \n
 *
 * Input data is given in the form of comma-separated indices. \n \n
 *
 * On error (invalid input data), prints an error message and exits.
 *
 * @param data the data to parse
 */
static std::unordered_set<int32_t> parse_data(wchar_t *data) {
    std::unordered_set<int32_t> parsed_indices;

    wchar_t *wcstok_buffer;
    for (wchar_t *token = wcstok_s(data, L",", &wcstok_buffer);
         token != NULL;
         token = wcstok_s(NULL, L",", &wcstok_buffer)) {
        try {
            int32_t index = std::stoi(token);
            parsed_indices.insert(index);
        } catch ([[maybe_unused]] std::invalid_argument const& ex) {
            parse_error(L"Index " + std::wstring(token) + L" is not valid.");
        } catch ([[maybe_unused]] std::out_of_range const& ex) {
            parse_error(L"Index " + std::wstring(token) + L" is out of range.");
        }
    }

    if (parsed_indices.empty()) {
        parse_error(L"No data supplied.");
    }

    return parsed_indices;
}

/**
 * Parses an option. If the input string is NULL (i.e. no option flag was provided), returns the default option of
 * DELETE. If the option is -e, returns DELETE_EXISTING. If the option is -a, returns DELETE_ALL. If the option
 * has any other value, prints an error message and exits.
 *
 * @param option the option to parse
 * @return an option, specified by the argument
 */
static Mode parse_option(wchar_t *option) {
    if (option == NULL) {
        return WEGAPI_DELETE;
    } else if (wcsncmp(option, L"-e", 3) == 0) { // compare 1 extra char so we don't get false positives
        return WEGAPI_DELETE_EXISTING;
    } else if (wcsncmp(option, L"-a", 3) == 0) { // compare 1 extra char so we don't get false positives
        return WEGAPI_DELETE_ALL;
    } else {
        parse_error(L"Unrecognized option.");
    }
}

/**
 * Get the path of a tile, from the game directory and its index. \n \n
 *
 * On error, prints an error message and exits.
 *
 * @param game_dir the game directory
 * @param index the index of the tile to get the path of
 * @return the path of the tile
 */
static wchar_t *get_tile_path(wchar_t *game_dir, int32_t index) {
    // todo: INCORPORATE VISIBLE NAME
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
 * Deletes a tile with a specified index. Returns whether the operation succeeded. \n \n
 *
 * This function should not be called with a mode of WEGAPI_DELETE_ALL. Attempting to do so will print an error
 * and exit.
 *
 * @param game_dir the game directory
 * @param index index of the tile to delete
 * @param mode mode (how to handle existing tiles, etc.)
 * @return whether the file was successfully deleted
 */
static bool delete_tile(wchar_t *game_dir, int index, Mode mode) {
    wchar_t *tile_path = get_tile_path(game_dir, index);

    switch (mode) {
        case WEGAPI_DELETE:
            if (!DeleteFileW(tile_path)) {
                DWORD error = GetLastError();
                if (error == ERROR_FILE_NOT_FOUND) {
                    return true;
                } else {
                    wegapi::util::print_last_error((L"DeleteFileW on " + std::wstring(tile_path)).c_str(), false);
                    return false;
                }
            } else {
                return true;
            }
        case WEGAPI_DELETE_EXISTING:
            if (!DeleteFileW(tile_path)) {
                wegapi::util::print_last_error((L"DeleteFileW on " + std::wstring(tile_path)).c_str(), false);
                return false;
            } else {
                return true;
            }
        case WEGAPI_DELETE_ALL:
            std::wcout << L"Error: delete_tile called with WEGAPI_DELETE_ALL mode." << std::endl;
            exit(EXIT_FAILURE);
        default:
            std::wcout << L"Unrecognized mode in delete_tile." << std::endl;
            exit(EXIT_FAILURE);
    }
    //SHChangeNotify(SHCNE_DELETE, SHCNF_PATH | SHCNF_FLUSHNOWAIT, tile_path, NULL);
}

/**
 * Deletes tiles from the data passed in to the program at the command line, retrying a certain number of times.
 * Destructively modifies the tiles_to_delete parameter.\n \n
 *
 * On error, prints a message and exits.
 *
 * @param game_dir the game directory
 * @param data a set of tile indices to delete - DESTRUCTIVELY MODIFIED
 * @param mode mode (how to handle existing tiles, etc.)
 */
static void delete_tiles_retry(wchar_t *game_dir, std::unordered_set<int32_t>& tiles_to_delete, Mode mode) {
    for (int i = 0; i < NUM_DELETE_RETRIES; i++) {
        for (auto iterator = tiles_to_delete.begin(); iterator != tiles_to_delete.end(); ) {
            bool success = delete_tile(game_dir, *iterator, mode);
            if (success) {
                iterator = tiles_to_delete.erase(iterator);
            } else {
                iterator++;
            }
        }
        if (tiles_to_delete.empty()) {
            break;
        }
        Sleep(DELETE_RETRY_DELAY);
    }

    if (!tiles_to_delete.empty()) {
        std::wcout << L"Could not delete the following tiles after " << NUM_DELETE_RETRIES << " retries: \n";
        for (int32_t index : tiles_to_delete) {
            std::wcout << std::wstring(get_tile_path(game_dir, index)) << "\n";
        }
        std::wcout << std::endl;
        exit(EXIT_FAILURE);
    }
}

/**
 * Get the path to search for all .exe files in a directory, for use with FindFirstFileW(). This is the path dir\*.exe. \n \n
 *
 * On error, prints an error message and exits.
 *
 * @param dir the directory
 * @return a search path for all .exe files in a directory
 */
static wchar_t *get_exe_search_path(wchar_t *dir) {
    wchar_t *search_path = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    wcscpy_s(search_path, 1+_MAX_PATH, dir);

    HRESULT hr = PathCchAppend(search_path, 1+_MAX_PATH, L"*.exe");

    if (!wegapi::util::check_success(hr, L"PathCchAppend")) {
        exit(EXIT_FAILURE);
    }

    return search_path;
}

/**
 * Deletes all tiles in the game directory. On error, prints an error message and exits.
 *
 * @param game_dir the game directory
 */
static void delete_all_tiles(wchar_t *game_dir) {
    WIN32_FIND_DATA find_data;
    wchar_t *search_path = get_exe_search_path(game_dir);
    HANDLE search_handle = FindFirstFileW(search_path, &find_data);

    if (search_handle == INVALID_HANDLE_VALUE) {
        wegapi::util::print_last_error(L"FindFirstFileW", true);
        exit(EXIT_FAILURE);
    }

    std::unordered_set<std::wstring> paths_to_delete;
    wchar_t *path_to_delete = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    while (true) {
        wcscpy_s(path_to_delete, 1+_MAX_PATH, game_dir);
        HRESULT hr = PathCchAppend(path_to_delete, 1+_MAX_PATH, find_data.cFileName);
        if (!wegapi::util::check_success(hr, L"PathCchAppend")) {
            exit(EXIT_FAILURE);
        }
        paths_to_delete.insert(std::wstring(path_to_delete));

        if (!FindNextFileW(search_handle, &find_data)) {
            DWORD error = GetLastError();
            if (error == ERROR_NO_MORE_FILES) {
                break;
            } else {
                wegapi::util::print_last_error(L"FindNextFileW", true);
                exit(EXIT_FAILURE);
            }
        } // if this succeeds, we can go to the next iteration of the loop and process it
    }

    // now, we have all paths to delete
    for (int i = 0; i < NUM_DELETE_RETRIES; i++) {
        for (auto iterator = paths_to_delete.begin(); iterator != paths_to_delete.end(); ) {
            if (!DeleteFileW(iterator->c_str())) {
                DWORD error = GetLastError();
                if (error == ERROR_FILE_NOT_FOUND) {
                    // very strange
                    std::wcout << L"DeleteFileW on path failed because file was not found, but file path was obtained from FindFirstFileW/FindNextFileW" << std::endl;
                } else {
                    wegapi::util::print_last_error((L"DeleteFileW on " + *iterator).c_str(), false);
                }
                iterator++;
            } else {
                iterator = paths_to_delete.erase(iterator);
            }
        }

        if (paths_to_delete.empty()) {
            break;
        }
        Sleep(DELETE_RETRY_DELAY);
    }

    if (!paths_to_delete.empty()) {
        std::wcout << L"Could not delete the following tiles after " << NUM_DELETE_RETRIES << " retries: \n";
        for (auto& path : paths_to_delete) {
            std::wcout << path << "\n";
        }
        std::wcout << std::endl;
        wegapi::util::wait_for_user();
        exit(EXIT_FAILURE);
    }
}

/**
 * Deletes tiles from the data passed in to the program at the command line. Destructively modifies the
 * tiles_to_delete parameter. \n \n
 *
 * On error, prints a message and exits.
 *
 * @param game_dir the game directory
 * @param data a set of tile indices to delete - DESTRUCTIVELY MODIFIED
 * @param mode mode (how to handle existing tiles, etc.)
 */
static void delete_tiles(wchar_t *game_dir, std::unordered_set<int32_t>& tiles_to_delete, Mode mode) {
    switch (mode) {
        case WEGAPI_DELETE:
        case WEGAPI_DELETE_EXISTING:
            delete_tiles_retry(game_dir, tiles_to_delete, mode);
            return;
        case WEGAPI_DELETE_ALL:
            delete_all_tiles(game_dir);
            return;
    }
}

/**
 * Tells Windows Explorer that the game directory changed, so that it knows some files were deleted. This function is
 * registered to run at exit with atexit(), so that if the program terminates at any point any already completed changes
 * are shown. E.g. if the third deletion causes an exit, the first two deletions which have already happened are shown
 * to the user.
 */
static void notify_explorer() {
    SHChangeNotify(SHCNE_UPDATEDIR, SHCNF_PATH | SHCNF_FLUSH, game_dir_global, NULL);

    //SHChangeNotify(SHCNE_ASSOCCHANGED, SHCNF_IDLIST, NULL, NULL);

    /*
    for (auto& tile : tiles_changed) {
        std::wcout << L"notify_explorer called on " << tile << std::endl;
        SHChangeNotify(SHCNE_UPDATEITEM, SHCNF_PATH | SHCNF_FLUSHNOWAIT, tile.c_str(), NULL);
    }
    std::wcout << L"notify_explorer called on " << std::wstring(game_dir_global) << std::endl;
    SHChangeNotify(SHCNE_UPDATEDIR, SHCNF_PATH | SHCNF_FLUSH, game_dir_global, NULL);
    std::wcout << L"done!" << std::endl;
     */
}

/**
 * Main function. Creates tiles based on command-line arguments.
 *
 * @param argc number of arguments
 * @param argv[1] the directory in which to delete tiles
 * @param argv[2] a comma-separated string of tile indices to delete
 * @param argv[3] optional: a mode option (either -e or -a). -e specifies that tiles should only be deleted if they
 *                already exist. -a specifies all tiles should be deleted.
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

    if (!wegapi::util::check_exists_perror(game_dir, L"delete_tiles: game directory doesn't exist")) {
        exit(EXIT_FAILURE);
    }

    Mode mode = parse_option(option);

    std::unordered_set<int32_t> tiles_to_delete;
    if (mode != WEGAPI_DELETE_ALL)  {
        tiles_to_delete = parse_data(data);
    }

    print_args(game_dir, tiles_to_delete, mode);

    // installs exit handler to refresh Windows Explorer on exit, so that even on abnormal exit updates are shown to the player
    size_t game_dir_len = 1+wcslen(game_dir);
    game_dir_global = (wchar_t*)malloc(sizeof(wchar_t) * game_dir_len);
    wcscpy_s(game_dir_global, game_dir_len, game_dir);
    atexit(notify_explorer);

    delete_tiles(game_dir, tiles_to_delete, mode);

    exit(EXIT_SUCCESS);
}