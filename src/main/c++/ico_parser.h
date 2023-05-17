#pragma once

#include <Windows.h>
#include <vector>

namespace wegapi {
    namespace icons {
        /**
         * Struct that contains all the information needed to create/update an RT_ICON resource with UpdateResource(). \n \n
         *
         * The call to UpdateResource() should use: \n
         *   - RT_ICON as lpType \n
         *   - MAKEINTRESOURCE(resource_number) as lpName \n
         *   - data as lpData \n
         *   - size as cb
         */
        typedef struct {
            DWORD size;
            BYTE *data;
            DWORD resource_number;
        } RT_ICON_DATA;

        /**
         * Class that contains all the information needed to create/update an RT_GROUP_ICON resource with UpdateResource().
         * This is a class for automatic management of the vector member. \n \n
         *
         * One call to UpdateResource() should be made for the group icon header, which should use the following paramters: \n
         *   - RT_GROUP_ICON as lpType \n
         *   - MAKEINTRESOURCE(0) as lpType (ensures that updates overwrite previous resources) \n
         *   - header as lpData \n
         *   - header_size as cb \n \n
         *
         * Then, for element in the images vector, a call should be made to UpdateResource() as described in the
         * RT_ICON_DATA struct.
         */
        class RT_GROUP_ICON_DATA {
            public:
                BYTE *header;
                DWORD header_size;
                std::vector<RT_ICON_DATA> images;
        };

        /**
        * Parses a .ico file, and converts it into the format required by a RT_GROUP_ICON and associated RT_ICON
        * resources in a PE (Portable Executable) file. \n \n
        *
        * See ico_parser.h (above) for more information on the structure of the return value. \n \n
        *
        * On error, prints a message and exits.
        *
        * @param icon_memory_mapping a .ico file, memory-mapped
        * @param icon_path the path of the .ico file, for error messages
        *
        * @return a RT_GROUP_ICON_DATA, which contains all the information required to change the icon of an executable, via
        *         UpdateResource() - see ico_parser.h (above) for more info
        */
        RT_GROUP_ICON_DATA ico_to_icon_resource(wchar_t *icon_path);
    }
}

