#include "constants.h"
#include <Windows.h>
#include <string>
#include <iostream>
#include <io.h>
#include <fcntl.h>
#include <shobjidl.h>
#include <comdef.h>

/**
 * Waits for user input, so that they can read any error output. Intended for debugging/development.
 */
static void wait_for_user() {
    std::wcout << L"An error has occurred, and execution has been halted so that you can read the output. Enter anything to continue." << std::endl;
    int i;
    std::wcin >> i;
}

static wchar_t *get_wegapi_jar() {
    const wchar_t wegapi_filename[] = L"wegapi";
    const wchar_t wegapi_fileext[] = L"jar";

    wchar_t *wpgmptr;
    _get_wpgmptr(&wpgmptr); // initialized to the path of the currently running executable (i.e. our path)
    wchar_t *drive = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_DRIVE));
    wchar_t *dir_without_drive = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_DIR));
    if (_wsplitpath_s(wpgmptr, drive, 1+_MAX_DRIVE, dir_without_drive, 1+_MAX_DIR, NULL, 0, NULL, 0) != 0) {
        _wperror(L"Splitting my dir failed");
        wait_for_user();
        exit(EXIT_FAILURE); // todo: instead search for default install
    }

    wchar_t *wegapi_path = (wchar_t*)malloc(sizeof(wchar_t) * (1+_MAX_PATH));
    if (_wmakepath_s(wegapi_path, 1+_MAX_PATH, drive, dir_without_drive, wegapi_filename, wegapi_fileext)) {
        _wperror(L"Making path failed");
        wait_for_user();
        exit(EXIT_FAILURE); // todo: instead search for default install
    }

    if (GetFileAttributesW(wegapi_path) == INVALID_FILE_ATTRIBUTES) {
        DWORD error = GetLastError();
        std::string error_message = std::system_category().message(error);
        std::wstring error_message_wstring(error_message.begin(), error_message.end());
        std::wcout << error_message_wstring << std::endl;
        wait_for_user();
        exit(EXIT_FAILURE); // todo: instead search for default install
    }

    return wegapi_path;
}

static bool check_success(HRESULT hr) {
    bool success = SUCCEEDED(hr);

    if (!success) {
        _com_error error(hr);
        LPCWSTR errorMessage = error.ErrorMessage();
        _setmode(_fileno(stdout), _O_U16TEXT);
        std::wcout << "error: ";
        wprintf(errorMessage);
        std::wcout << "\n" << std::endl;
        wait_for_user();
    }

    return success;
}

// style isn't the best but I'll write to justify it later
static wchar_t *getFileFromUser() {
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

    hr = fileOpenDialog->SetOptions(flags | FOS_PICKFOLDERS | FOS_NOCHANGEDIR | FOS_FORCEFILESYSTEM | FOS_OKBUTTONNEEDSINTERACTION);

    if (!check_success(hr)) goto cleanup_releaseFileDialog;

    hr = fileOpenDialog->Show(NULL);

    if (!check_success(hr)) goto cleanup_releaseFileDialog;

    IShellItem *shellItem;
    hr = fileOpenDialog->GetResult(&shellItem);

    if (!check_success(hr)) goto cleanup_releaseFileDialog;

    hr = shellItem->GetDisplayName(SIGDN_FILESYSPATH, &filePath);
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

int wmain() {
    _setmode(_fileno(stdout), _O_U16TEXT);
    wchar_t *wegapi_jar = get_wegapi_jar();

    wprintf(wegapi_jar);
    std::wcout << "\n" << std::endl;
    int i;

    wchar_t *game_dir = getFileFromUser();
    wprintf(game_dir);
    std::wcout << "\n" << std::endl;
    std::wcin >> i;

    CoTaskMemFree(game_dir);
}