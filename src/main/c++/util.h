#pragma once

#include <Windows.h>

/**
 * Comments in implementation (util.cpp), as this is developer-facing code.
 */
namespace wegapi {
    namespace java {
        wchar_t *get_java_path();
        void launch_java(wchar_t *java_path, wchar_t *cmdline);
    }

    namespace filenames {
        int32_t filename_to_index(wchar_t *filename);
        wchar_t *index_to_filename(int32_t index);
        wchar_t *index_to_filename_with_exe(int32_t index);
    }

    namespace util {
        // todo: message box errors
        void wait_for_user();
        void print_last_error(const wchar_t *message);
        // todo: maybe change to wstring
        bool path_exists(wchar_t *path);
        bool check_exists_perror(wchar_t *path, const wchar_t *error_message);
        bool check_success(HRESULT hr, const wchar_t *error_message);
    }
}