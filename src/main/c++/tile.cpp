#include <Windows.h>
#include <string>
#include <fcntl.h>

#include "constants.h"
#include "util.h"

// mallocs are not cleaned up as we will exit soon
// todo: check more errors

// todo: convert names to zero width space

// testing parsing unicode filenames: cl /std:c++17 /EHsc tile.cpp -o 诶娜迪艾伊吾.exe

/**
 * Gets the filename of the currently running executable. For example, if the currently running executable is located
 * at "C:\Windows\System32\filename.exe", this would return "filename". \n \n
 *
 * The currently running executable must have a filename of length wegapi::filenames::FILENAME_LENGTH.\n \n
 *
 * On error, prints a message and aborts.
 *
 * @return the filename of the currently running executable, as a null-terminated wide character string
 */
static wchar_t *get_my_filename() {
    wchar_t *wpgmptr;
    _get_wpgmptr(&wpgmptr); // initialized to the path of the currently running executable (i.e. our path)
    wchar_t *my_filename = (wchar_t*)malloc(sizeof(wchar_t) * (1+wegapi::filenames::FILENAME_LENGTH));
    if (_wsplitpath_s(wpgmptr, NULL, 0, NULL, 0, my_filename, 1+wegapi::filenames::FILENAME_LENGTH, NULL, 0) != 0) {
        _wperror(L"Splitting my_filename failed");
        wegapi::util::wait_for_user();
        exit(EXIT_FAILURE);
    }
    return my_filename;
}

/**
 * Gets the filename of an executable located at `path`. For example, if the input argument is
 * "C:\Windows\System32\filename.exe", this would return "filename". \n \n
 *
 * The supplied argument must have a filename of length wegapi::filenames::FILENAME_LENGTH.\n \n
 *
 * On error, prints a message and aborts.
 *
 * @param path path to get a filename from
 * @return the filename of an executable located at `path`, as a null-terminated wide character string
 */
static wchar_t *get_other_filename(wchar_t *path) {
    // todo: perhaps enforce that this is different from my_filename
    wchar_t *other_filename = (wchar_t*)malloc(sizeof(wchar_t) * (1+wegapi::filenames::FILENAME_LENGTH));
    if (_wsplitpath_s(path, NULL, 0, NULL, 0, other_filename, 1+wegapi::filenames::FILENAME_LENGTH, NULL, 0) != 0) {
        _wperror(L"Splitting other_filename failed");
        wegapi::util::wait_for_user();
        exit(EXIT_FAILURE);
    }
    return other_filename;
}



/**
 * Launches the Java client, where the user has dragged one executable to another.
 *
 * @param java_path path to a Java executable
 * @param from_index index of executable that was dragged
 * @param to_index index of executable that the from_index executable was dragged to
 */
static void launch_java_dragged(wchar_t *java_path, int from_index, int to_index) {
    int cmdline_size = _scwprintf(wegapi::java::JAVA_CMDLINE_DRAGGED, from_index, to_index);
    wchar_t *cmdline = (wchar_t*)malloc(sizeof(wchar_t) * (1+cmdline_size));

    if (swprintf(cmdline, 1+cmdline_size, wegapi::java::JAVA_CMDLINE_DRAGGED, from_index, to_index) == -1) {
        _wperror(L"swprintf failed for dragged command-line parameters");
        wegapi::util::wait_for_user();
        exit(EXIT_FAILURE);
    }

    wegapi::java::launch_java(java_path, cmdline);
}

/**
 * Launches the Java client, where the user double clicked an executable.
 *
 * @param java_path path to a Java executable
 * @param clicked_index index of the executable that was clicked
 */
static void launch_java_clicked(wchar_t *java_path, int clicked_index) {
    int cmdline_size = _scwprintf(wegapi::java::JAVA_CMDLINE_CLICKED, clicked_index);
    wchar_t *cmdline = (wchar_t*)malloc(sizeof(wchar_t) * (1+cmdline_size));

    if (swprintf(cmdline, 1+cmdline_size, wegapi::java::JAVA_CMDLINE_CLICKED, clicked_index) == -1) {
        _wperror(L"swprintf failed for dragged command-line parameters");
        wegapi::util::wait_for_user();
        exit(EXIT_FAILURE);
    }

    wegapi::java::launch_java(java_path, cmdline);
}

/**
 * Main function. Parses arguments and calls Java Client code based on command line arguments. \n \n
 *
 * This leverages the following behavior of Windows Explorer: dragging a file onto an executable runs that executable
 * with the dragged file as a command-line argument. \n \n
 *
 * For example, dragging B.exe onto A.exe will have argv[1] = "B.exe" (actually, fully qualified paths are used, and
 * are stripped appropriately).  \n \n
 *
 * We can then use this command-line argument to detect whether a user double-clicked an executable (which will have
 * only 1 argument), or dragged one executable onto another (which will have 2 arguments), and we can tell which
 * executables were involved via their paths. \n \n
 *
 * Memory allocations are not freed, as this program is intended to be short-lived. Allocations will be cleaned
 * up by the OS on exit.
 *
 * @param argc number of arguments - 1 or 2
 * @param argv argv[1] (OPTIONAL): path of another tile executable in the same directory
 */
int wmain(int argc, wchar_t* argv[]) {
    using namespace std; // todo: remove

    // First, we find the name of our executable
    wchar_t *my_filename = get_my_filename();

    // Then, we find java
    wchar_t *java_path = wegapi::java::get_java_path();

    int my_index = wegapi::filenames::filename_to_index(my_filename);

    if (argc == 2) {
        wchar_t *other_filename = get_other_filename(argv[1]);
        int other_index = wegapi::filenames::filename_to_index(other_filename);
        launch_java_dragged(java_path, other_index, my_index);
        exit(EXIT_SUCCESS);
    } else if (argc == 1) {
        launch_java_clicked(java_path, my_index);
        exit(EXIT_SUCCESS);
    } else {
        MessageBoxW(NULL, (LPCWSTR)L"Invalid number of arguments.", NULL, MB_ICONERROR | MB_OK);
        exit(EXIT_FAILURE);
    }
}