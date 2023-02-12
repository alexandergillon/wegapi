#pragma once
#include <unordered_map>

namespace wegapi {
    namespace filenames {
        const int FILENAME_LENGTH = 6;

        /* const wchar_t JAR_FILENAME[] = L"wegapi";
        const wchar_t GAMEDATA_DIR[] = L".gamedata";
        const wchar_t JAVA_CLIENT_CLASSNAME[] = L"com.github.alexandergillon.wegapi.client.Client"; */

        // The space at the front of these literals is REQUIRED. Else java will not receive parameters correctly.
        const wchar_t JAVA_CMDLINE_CLICKED[] = L" -cp .\\.gamedata\\wegapi.jar com.github.alexandergillon.wegapi.client.Client -c%d";
        const wchar_t JAVA_CMDLINE_DRAGGED[] = L" -cp .\\.gamedata\\wegapi.jar com.github.alexandergillon.wegapi.client.Client -d%d -t%d";


        namespace characters {
            // using abcde for testing purposes 
            const wchar_t three_per_em_space = L'a'; // L'\u2004';
            const wchar_t four_per_em_space = L'b'; // L'\u2005';
            const wchar_t six_per_em_space = L'c'; // L'\u2006';
            const wchar_t thin_space = L'd'; // L'\u2009';
            const wchar_t hair_space = L'e'; // L'\u200A';

            static const std::unordered_map<wchar_t, int> wchar_to_sort_order = {
                {three_per_em_space, 0},
                {four_per_em_space, 1},
                {six_per_em_space, 2},
                {thin_space, 3},
                {hair_space, 4}
            };

            static const std::unordered_map<wchar_t, int> sort_order_to_wchar = {
                {0, three_per_em_space},
                {0, four_per_em_space},
                {0, six_per_em_space},
                {0, thin_space},
                {0, hair_space}
            };
        }
    }
}
