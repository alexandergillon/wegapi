#include <Windows.h>
#include <string>
#include <iostream>
#include <regex>
#include <io.h>
#include <fcntl.h>
#include <shobjidl.h>
#include <comdef.h>

#include "constants.h"
#include "helpers.h"

/**
 * Gets the path of the "wegapi.jar" file, which contains all WEGAPI Java code. This file is assumed to be in the
 * same directory as the current executable. This will likely change in the future (or, backstops may be implemented
 * to check some default locations if the .jar cannot be found in the same directory). \n \n
 *
 * On error, prints a message and aborts.
 *
 * @return the path of the "wegapi.jar" file
 */
static wchar_t *get_wegapi_jar() {
    const wchar_t wegapi_filename[] = L"wegapi";
    const wchar_t wegapi_fileext[] = L"jar";

    // Get our current directory
    wchar_t *wpgmptr;
    _get_wpgmptr(&wpgmptr); // initialized to the path of the currently running executable by the OS
    wchar_t *drive = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_DRIVE));
    wchar_t *dir_without_drive = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_DIR));
    if (_wsplitpath_s(wpgmptr, drive, 1+_MAX_DRIVE, dir_without_drive, 1+_MAX_DIR, NULL, 0, NULL, 0) != 0) {
        _wperror(L"Splitting my dir failed");
        wegapi::wait_for_user();
        exit(EXIT_FAILURE); // todo: instead search for default install
    }

    // Get the path where wegapi.jar should be
    wchar_t *wegapi_path = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    if (_wmakepath_s(wegapi_path, 1+_MAX_PATH, drive, dir_without_drive, wegapi_filename, wegapi_fileext)) {
        _wperror(L"Making path failed");
        wegapi::wait_for_user();
        exit(EXIT_FAILURE); // todo: instead search for default install
    }

    // Check it actually exists
    if (GetFileAttributesW(wegapi_path) == INVALID_FILE_ATTRIBUTES) {
        DWORD error = GetLastError();
        std::string error_message = std::system_category().message(error);
        std::wstring error_message_wstring(error_message.begin(), error_message.end());
        std::wcout << error_message_wstring << std::endl;
        wegapi::wait_for_user();
        exit(EXIT_FAILURE); // todo: instead search for default install
    }

    return wegapi_path;
}

/**
 * Checks whether a Win32 API call succeeded, based on its return value. If it failed, prints an error message,
 * and pauses execution so that the user can read the error message. \n \n
 *
 * @param hr the return value of a Win32 API call
 * @return whether that call succeeded
 */
static bool check_success(HRESULT hr) {
    bool success = SUCCEEDED(hr);

    if (!success) {
        _com_error error(hr);
        LPCWSTR errorMessage = error.ErrorMessage();

        _setmode(_fileno(stdout), _O_U16TEXT);
        std::wcout << "error: ";
        wprintf(errorMessage);
        std::wcout << std::endl;

        wegapi::wait_for_user();
    }

    return success;
}

/**
 * Allows the user to pick a folder in the filesystem, and returns its path. If the user did not pick a folder (by
 * exiting the dialog shown to them), or if an error occurred, prints an error message and returns NULL. \n \n
 *
 * This function is based an example from Microsoft, found here: \n
 *   https://learn.microsoft.com/en-us/windows/win32/learnwin32/example--the-open-dialog-box \n
 * I'm sure I don't understand all the intricacies of it, as the Win32 API is rather complex, but it works. \n \n
 *
 * This function commits the 'cardinal sin' of programming: goto. This was a deliberate choice, and not taken lightly.
 * However, because of the many Win32 API calls that we need to check the success of, other forms of control flow
 * become rather messy and unreadable. See the following, provided by Microsoft, for an example: \n
 *   https://learn.microsoft.com/en-us/windows/win32/shell/common-file-dialog \n
 * I personally think that using gotos, while usually frowned upon, allows a reader to more easily follow the actual
 * flow of control much better here.
 *
 * @return the path of the folder that the user chose, or NULL if they didn't choose one/an error occurred
 */
static wchar_t *get_folder_from_user() {
    wchar_t *filePath = NULL;
    HRESULT hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);

    if (!check_success(hr)) goto nocleanup;

    IFileOpenDialog *fileOpenDialog;
    hr = CoCreateInstance(CLSID_FileOpenDialog, NULL, CLSCTX_ALL,
                          IID_IFileOpenDialog, reinterpret_cast<void**>(&fileOpenDialog));

    if (!check_success(hr)) goto cleanup_uninitialize;

    DWORD flags;
    hr = fileOpenDialog->GetOptions(&flags);

    if (!check_success(hr)) goto cleanup_releaseFileDialog;

    // only allow user to pick folders
    hr = fileOpenDialog->SetOptions(flags | FOS_PICKFOLDERS | FOS_NOCHANGEDIR | FOS_FORCEFILESYSTEM | FOS_OKBUTTONNEEDSINTERACTION);

    if (!check_success(hr)) goto cleanup_releaseFileDialog;

    hr = fileOpenDialog->Show(NULL); // show user the dialog

    if (!check_success(hr)) goto cleanup_releaseFileDialog;

    IShellItem *shellItem;
    hr = fileOpenDialog->GetResult(&shellItem);

    if (!check_success(hr)) goto cleanup_releaseFileDialog;

    hr = shellItem->GetDisplayName(SIGDN_FILESYSPATH, &filePath); // get the path of the folder they chose
    if (!check_success(hr)) {
        filePath = NULL;
    }

    shellItem->Release();
cleanup_releaseFileDialog:
    fileOpenDialog->Release();
cleanup_uninitialize:
    CoUninitialize();
nocleanup:
    return filePath;
}

/**
 * Converts the backslashes in a wide string to forwards slashes, in place. This works because both types of slash
 * are just one character, so we can do a direct replacement without reallocating buffers.
 *
 * @param s the string to replace slashes in
 */
static void back_to_forward_slashes(wchar_t *s) {
    std::wstring s_wstring = std::regex_replace(s, std::wregex(L"\\\\"), L"/");
    wcscpy_s(s, 1+ wcslen(s), s_wstring.c_str());
}

/**
 * Starts Java and launches the Client Daemon in the directory chosen by the user.
 *
 * @param java_path path to a Java executable
 * @param jar_path path to the "wegapi.jar" file
 * @param dir the directory that the user chose to start a game in
 */
static void launch_java(wchar_t *java_path, wchar_t *jar_path, wchar_t *dir) {
    back_to_forward_slashes(dir);
    int cmdline_size = _scwprintf(wegapi::java::JAVA_CMDLINE_START_CLIENT, jar_path, dir);
    wchar_t *cmdline = (wchar_t*)malloc(sizeof(wchar_t) * (1+cmdline_size));

    if (swprintf(cmdline, 1+cmdline_size, wegapi::java::JAVA_CMDLINE_START_CLIENT, jar_path, dir) == -1) {
        _wperror(L"swprintf failed for launch client command-line parameters");
        wegapi::wait_for_user();
        exit(EXIT_FAILURE);
    }

    wegapi::java::launch_java(java_path, cmdline);
}

/**
 * Main function. Allows the user to choose a directory, and then calls the Java client daemon to run, with that
 * directory as the game directory. \n \n
 *
 * Most allocations are not freed, as this program is intended to be short-lived. Allocations will be cleaned
 * up by the OS on exit. Some handles are freed: these are to do with Win32 API calls, as I'm not too knowledgeable
 * about the internals of the Win32 API and don't want to leave anything in a bad state. \n \n
 *
 * For now, this function assumes that the Java code (wegapi.jar) is located in the same directory as this
 * executable. This will likely change in the future (or, backstops may be implemented to check some default locations
 * if the .jar cannot be found in the same directory).
 *
 */
int wmain() {
    wchar_t *wegapi_jar = get_wegapi_jar();
    wchar_t *java_path = wegapi::java::get_java_path();
    wchar_t *game_dir = get_folder_from_user();

    launch_java(java_path, wegapi_jar, game_dir);
    CoTaskMemFree(game_dir);
}