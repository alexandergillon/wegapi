# cmake .. && cmake --build .
project(wegapi)

cmake_minimum_required(VERSION 3.0)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/../bin)

if(MSVC)
    add_compile_options(/EHsc /W4 /WX /DDEBUG /DUNICODE /D_UNICODE /fsanitize=address /Zi) # ASAN: /fsanitize=address /Zi
else()
    if(NOT WIN32)
        message(FATAL_ERROR "wegapi is only supported on Windows, as it relies on the Windows API.")
    endif()
endif()

add_executable(tile
        tile.cpp
        util.cpp)

# for running within CLion
add_executable(aaaaa
        tile.cpp
        util.cpp)

add_executable(start_client
        start_client.cpp
        util.cpp)

add_executable(create_tiles
        create_tiles.cpp
        util.cpp
        ico_parser.cpp)

target_link_libraries(create_tiles
        Shlwapi
        Pathcch)

add_executable(delete_tiles
        delete_tiles.cpp
        util.cpp)

target_link_libraries(delete_tiles
        Shlwapi
        Pathcch)