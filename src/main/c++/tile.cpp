#include "constants.h"
#include <Windows.h>
#include <string>
#include <iostream>
#include <io.h>
#include <fcntl.h>

// mallocs are not cleaned up as we will exit soon
// todo: check more errors

// TODO: fix not calling java correctly

// testing parsing unicode filenames: cl /std:c++17 /EHsc tile.cpp -o 诶娜迪艾伊吾.exe

namespace wegapi {
	static int32_t filename_to_index(wchar_t *filename) {
        using namespace std; // remove
        /* Length has already been validated in main
         * if (filename.length() != wegapi::filenames::FILENAME_LENGTH) {
            MessageBoxW(NULL, (LPCWSTR)L"Invalid filename length.", NULL, MB_ICONERROR | MB_OK);
            exit(EXIT_FAILURE);
        } */

        // todo: catch invalid character

        int32_t total = 0;
        int32_t pow = 1;

        for (int i = wegapi::filenames::FILENAME_LENGTH-1; i >= 0; i--) {
            total += wegapi::filenames::characters::wchar_to_sort_order.at(filename[i]) * pow;
            pow *= wegapi::filenames::FILENAME_LENGTH;
        }
        return total;
    }
}

int wmain(int argc, wchar_t* argv[])
{
    using namespace std; // remove
    wchar_t *wpgmptr;
    _get_wpgmptr(&wpgmptr);

    wchar_t *my_filename = (wchar_t*)malloc(sizeof(wchar_t) * (1+wegapi::filenames::FILENAME_LENGTH));
    if (_wsplitpath_s(wpgmptr, NULL, 0, NULL, 0, my_filename, 1+wegapi::filenames::FILENAME_LENGTH, NULL, 0) != 0) {
        // todo
        _wperror(L"");
        int i; std::cin >> i;
        exit(EXIT_FAILURE);
    }

    DWORD java_path_length = SearchPathW(NULL, L"java", L".exe", 0, NULL, NULL);
    if (java_path_length == 0) {
        // todo: java not found
        _wperror(L"");
        int i; std::cin >> i;
        exit(EXIT_FAILURE);
    }
    wchar_t *java_path = (wchar_t*)malloc(sizeof(wchar_t ) * java_path_length);
    SearchPathW(NULL, L"java", L".exe", java_path_length, java_path, NULL); // TOCTOU

    int my_index = wegapi::filename_to_index(my_filename);

    if (argc > 1) {
        wchar_t *other_filename = (wchar_t*)malloc(sizeof(wchar_t) * (1+wegapi::filenames::FILENAME_LENGTH));
        if (_wsplitpath_s(argv[1], NULL, 0, NULL, 0, other_filename, 1+wegapi::filenames::FILENAME_LENGTH, NULL, 0) != 0) {
            // todo
            _wperror(L"");
            int i; std::cin >> i;
            exit(EXIT_FAILURE);
        }
        int other_index = wegapi::filename_to_index(other_filename);
        int cmdline_size = _scwprintf(wegapi::filenames::JAVA_CMDLINE_DRAGGED, my_index, other_index);

        wchar_t *cmdline = (wchar_t*)malloc(sizeof(wchar_t) * (1+cmdline_size));
        if (swprintf(cmdline, 1+cmdline_size, wegapi::filenames::JAVA_CMDLINE_DRAGGED, other_index, my_index) == -1) {
            // todo: handle error
            _wperror(L"");
            int i; std::cin >> i;
            exit(EXIT_FAILURE);
        }

        wcout << cmdline << "\n" << endl;
        int j; cin >> j;
        STARTUPINFOW *startupInfo = (STARTUPINFOW*)calloc(1, sizeof(STARTUPINFO));
        PROCESS_INFORMATION *processInfo = (PROCESS_INFORMATION*)calloc(1, sizeof(PROCESS_INFORMATION));
        if (!CreateProcessW(java_path, cmdline, NULL, NULL, false,  0 /*CREATE_DEFAULT_ERROR_MODE | DETACHED_PROCESS*/, NULL, NULL, startupInfo, processInfo)) {
            // todo: handle error
            _wperror(L"");
            int i; std::cin >> i;
            exit(EXIT_FAILURE);
        }
        int i; std::cin >> i;
        exit(EXIT_SUCCESS);
    } else {
        // todo
        int cmdline_size = _scwprintf(wegapi::filenames::JAVA_CMDLINE_CLICKED, my_index);
        wchar_t *cmdline = (wchar_t*)malloc(sizeof(wchar_t) * (1+cmdline_size));

        if (swprintf(cmdline, 1+cmdline_size, wegapi::filenames::JAVA_CMDLINE_CLICKED, my_index) == -1) {
            // todo: handle error
            _wperror(L"");
            int i; std::cin >> i;
            exit(EXIT_FAILURE);
        }

        wcout << cmdline << "\n" << endl;
        int j; cin >> j;
        STARTUPINFOW *startupInfo = (STARTUPINFOW*)calloc(1, sizeof(STARTUPINFO));
        PROCESS_INFORMATION *processInfo = (PROCESS_INFORMATION*)calloc(1, sizeof(PROCESS_INFORMATION));

        if (!CreateProcessW(java_path, cmdline, NULL, NULL, false,  0 /*CREATE_DEFAULT_ERROR_MODE | DETACHED_PROCESS*/, NULL, NULL, startupInfo, processInfo)) {
            // todo: handle error
            _wperror(L"");
            int i; std::cin >> i;
            exit(EXIT_FAILURE);
        }
        int i; std::cin >> i;
        exit(EXIT_SUCCESS);
    }

    _setmode(_fileno(stdout), _O_U16TEXT);
    wprintf(java_path);
    wcout << "\n" << endl;

    int i;
    cin >> i;
}