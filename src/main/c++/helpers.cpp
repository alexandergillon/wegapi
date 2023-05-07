#include <iostream>

#include "helpers.h"
#include "Windows.h"

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

    /**
    * Waits for user input, so that they can read any error output. Intended for debugging/development.
    */
    void wait_for_user() {
        std::wcout << L"An error has occurred, and execution has been halted so that you can read the output. Enter anything to continue." << std::endl;
        int i;
        std::wcin >> i;
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
        using namespace std;
        if (GetFileAttributesW(path) == INVALID_FILE_ATTRIBUTES) {
            wstring error_message_user_w(error_message_user);
            wcout << error_message_user_w << ":\n\t";

            DWORD error = GetLastError();
            string error_message_sys = std::system_category().message(error);
            wstring error_message_sys_w(error_message_sys.begin(), error_message_sys.end());
            wcout << error_message_sys_w << endl;
            wait_for_user();
            return false;
        }
        return true;
    }
}
