#include <iostream>
#include <Windows.h>
#include <comdef.h>
#include <fcntl.h>
#include <io.h>

#include "helpers.h"
#include "constants.h"

namespace wegapi {
    namespace java {
        /**
        * Finds a path for a Java executable, found via the PATH. No restrictions are placed on the Java executable that this
        * function returns (e.g. Java version). If Java cannot be found, prints an error message and aborts.\n \n
        *
        * @return a path for a Java executable, as a null-terminated wide character string
        */
        wchar_t *get_java_path() {
            // todo: perhaps enforce java version
            DWORD java_path_length = SearchPathW(NULL, L"java", L".exe", 0, NULL, NULL);
            wchar_t *java_path = (wchar_t*)malloc(sizeof(wchar_t ) * java_path_length);
            java_path_length = SearchPathW(NULL, L"java", L".exe", java_path_length, java_path, NULL);
            if (java_path_length == 0) {
                MessageBoxW(NULL, (LPCWSTR)L"Java was not found on the PATH. Please add Java to the PATH.", NULL, MB_ICONERROR | MB_OK);
                exit(EXIT_FAILURE);
            }
            return java_path;
        }

        void launch_java(wchar_t *java_path, wchar_t *cmdline) {
            STARTUPINFOW *startupInfo = (STARTUPINFOW*)calloc(1, sizeof(STARTUPINFO));
            PROCESS_INFORMATION *processInfo = (PROCESS_INFORMATION*)calloc(1, sizeof(PROCESS_INFORMATION));

        #ifdef DEBUG
            std::wcout << "Java command line:" << "\n";
            std::wcout << "\t" << cmdline << "\n" << std::endl;
            // For debugging, we don't want Java to run in a detached process, as we want to see its output.
            DWORD creationFlags = 0;
        #else
            DWORD creationFlags = CREATE_DEFAULT_ERROR_MODE | DETACHED_PROCESS;
        #endif

            if (!CreateProcessW(java_path, cmdline, NULL, NULL, false,  creationFlags, NULL, NULL, startupInfo, processInfo)) {
                _wperror(L"Launching Java failed");
                wegapi::wait_for_user();
                exit(EXIT_FAILURE);
            }

        #ifdef DEBUG
            std::wcout << L"Execution has been halted to read Java output. Enter anything to continue." << std::endl;
            int i;
            std::wcin >> i;
        #endif
        }
    }

    namespace filenames {
        /** Converts a filename to its lexicographic index in a directory, as if all possible filenames were present.
        * Possible filenames are permutations of a fixed size (wegapi::filenames::FILENAME_LENGTH), over a fixed set of
        * characters (defined in wegapi::filenames::characters).\n \n
        *
        * For example, if the character set is ABC, and filenames are of length 3, then:
        * \verbatim
        *   filename_to_index(L"AAA") = 0,
        *   filename_to_index(L"AAB") = 1,
        *   filename_to_index(L"AAC") = 2,
        *   filename_to_index(L"ABA") = 3
        * \endverbatim
        * and so on. \n \n
        *
        * Requires inputs to be of length wegapi::filenames::FILENAME_LENGTH to produce correct results. Else, index
        * is calculated as if only the first wegapi::filenames::FILENAME_LENGTH characters are present. Also
        * requires that inputs only use characters in the character set.
        *
        * @param filename filename to convert: must be of length wegapi::filenames::FILENAME_LENGTH, and only use
        *                 characters defined in wegapi::filenames::characters
        * @return index of that filename
        */
        int32_t filename_to_index(wchar_t *filename) {
        #ifdef DEBUG
            // Length has already been validated in main
            if (wcslen(filename) != wegapi::filenames::FILENAME_LENGTH) {
                std::wcout << wcslen(filename) << std::endl;
                MessageBoxW(NULL, (LPCWSTR)L"Invalid filename length.", NULL, MB_ICONERROR | MB_OK);
                exit(EXIT_FAILURE);
            }

            try {
        #endif

            int32_t total = 0;
            int32_t pow = 1;

            // Essentially, we view the filename as a base `wegapi::filenames::FILENAME_LENGTH` string
            for (int i = wegapi::filenames::FILENAME_LENGTH - 1; i >= 0; i--) {
                total += wegapi::filenames::characters::wchar_to_sort_order.at(filename[i]) * pow;
                pow *= wegapi::filenames::FILENAME_LENGTH;
            }
            return total;

        #ifdef DEBUG
            } catch ([[maybe_unused]] const std::out_of_range& _) {
                MessageBoxW(NULL, (LPCWSTR)L"Invalid characters in filename.", NULL, MB_ICONERROR | MB_OK);
                exit(EXIT_FAILURE);
            }
        #endif
        }

        wchar_t *index_to_filename(int32_t index) {
            wchar_t *filename = (wchar_t*)calloc(sizeof(wchar_t), wegapi::filenames::FILENAME_LENGTH + 1);

            for (int i = 0; i < wegapi::filenames::FILENAME_LENGTH; i++) {
                int32_t remainder = index % wegapi::filenames::FILENAME_LENGTH;
                wchar_t character = wegapi::filenames::characters::sort_order_to_wchar.at(remainder);
                filename[(wegapi::filenames::FILENAME_LENGTH - 1) - i] = character;
                index /= wegapi::filenames::FILENAME_LENGTH;
            }

            return filename;
        }

        wchar_t *index_to_filename_exe(int32_t index) {
            wchar_t *filename_without_exe = index_to_filename(index);
            size_t filename_with_exe_size = wcslen(filename_without_exe) + 4 + 1; // 4 for .exe, 1 for null terminator
            wchar_t *filename_with_exe = (wchar_t*)malloc(sizeof(wchar_t) * filename_with_exe_size);
            wcscpy_s(filename_with_exe, filename_with_exe_size, filename_without_exe);
            wcscat_s(filename_with_exe, filename_with_exe_size, L".exe");
            free(filename_without_exe);
            return filename_with_exe;
        }
    }

    /**
    * Waits for user input, so that they can read any error output. Intended for debugging/development.
    */
    void wait_for_user() {
        std::wcout << L"An error has occurred, and execution has been halted so that you can read the output. Enter anything to continue." << std::endl;
        int i;
        std::wcin >> i;
    }

    void print_last_error(const wchar_t *error_message) {
        DWORD error = GetLastError();
        std::string error_message_sys = std::system_category().message(error);
        std::wstring error_message_sys_w(error_message_sys.begin(), error_message_sys.end());
        std::wcout << std::wstring(error_message) << L": " << error_message_sys_w << std::endl;
        wait_for_user();
    }

    /**
     * Check whether a path exists. If it doesn't, prints an error message. A file may not 'exist' for reasons such
     * as invalid permissions, etc., in which case this function still prints an error and returns false.
     *
     * @param path path to check whether exists
     * @param error_message_user error message to print, if the file doens't exist
     * @return whether the file exists
     */
    bool check_exists(wchar_t *path, const wchar_t *error_message_user) {
        if (GetFileAttributesW(path) == INVALID_FILE_ATTRIBUTES) {
            std::wstring error_message_user_w(error_message_user);
            std::wcout << error_message_user_w << ":\n\t";
            print_last_error(L"check_exists");
            return false;
        }
        return true;
    }

    /**
    * Checks whether a Win32 API call succeeded, based on its return value. If it failed, prints an error message,
    * and pauses execution so that the user can read the error message. \n \n
    *
    * @param hr the return value of a Win32 API call
    * @param error_message a message to print, on error, along with a default error message
    * @return whether that call succeeded
    */
    bool check_success(HRESULT hr, const wchar_t *error_message) {
        // todo:rename params
        bool success = SUCCEEDED(hr);

        if (!success) {
            _com_error error(hr);
            LPCWSTR errorMessage = error.ErrorMessage();

            _setmode(_fileno(stdout), _O_U16TEXT);
            std::wcout << L"error (";
            wprintf(error_message); // this naming is awful, will change
            std::wcout << L"):";
            wprintf(errorMessage);
            std::wcout << std::endl;

            wegapi::wait_for_user();
        }

        return success;
    }
}
