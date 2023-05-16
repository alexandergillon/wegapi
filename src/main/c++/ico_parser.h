#pragma once

#include <Windows.h>
#include <vector>

namespace wegapi {
    namespace icons {
        typedef struct {
            DWORD size;
            BYTE *data;
            DWORD resource_number;
        } RT_ICON_DATA;

        class RT_GROUP_ICON_DATA {
            public:
                BYTE *header;
                DWORD header_size;
                std::vector<RT_ICON_DATA> images;
        };

        RT_GROUP_ICON_DATA ico_to_icon_resource(wchar_t *icon_path);
    }
}

