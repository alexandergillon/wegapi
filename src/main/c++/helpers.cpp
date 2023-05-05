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
    }

    /**
    * Waits for user input, so that they can read any error output. Intended for debugging/development.
    */
    void wait_for_user() {
        std::wcout << L"An error has occurred, and execution has been halted so that you can read the output. Enter anything to continue." << std::endl;
        int i;
        std::wcin >> i;
    }
}
