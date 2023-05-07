#pragma once

namespace wegapi {
    namespace java {
        wchar_t *get_java_path();
        void launch_java(wchar_t *java_path, wchar_t *cmdline);
    }

    void wait_for_user();
    bool check_exists(wchar_t *path, const wchar_t *error_message);
}