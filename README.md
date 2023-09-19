# Windows Explorer Game API (WEGAPI)

A framework for creating tile-based games within Windows Explorer.

Note: this is really more of a framework than an API. When I started this project, I thought this would be more of an API, but the intricacies of the Win32 API means that it works best as a framework. So the name stuck. I may change it later.

## Description

The idea for this project was to use Windows Explorer to do something that it really wasn't intended to do: to play games. If you think about it, Windows Explorer instances are really just a grid of tiles (when you view files with icons, rather than as a list). As a result, if we could get those tiles to display custom images, and gave a player a way to interact with the images, then we could implement a tile-based game. The first game I have chosen to implement with this framework is chess, example below:

![Example Image of a WEGAPI Chess Game](https://github.com/alexandergillon/wegapi/blob/abc699dda33b64a38e9a8a855d253be490bb0827/src/main/java/com/github/alexandergillon/wegapi/server/chess/chess.png?raw=true)

This framework is designed to be usable to implement almost any tile-based game, by simply implementing a specific interface in Java which processes game events and stores the game state. 

While the idea for this project is (to my knowledge) original, the spirit  of this project is definitely inspired by Tom7 ([website](http://tom7.org/), [youtube](https://www.youtube.com/@tom7)), who likes to do things in impractical ways and use technologies for things they weren't intended for. Check him out!

**Note: this project is still in progress, and a little rough around the edges. I am a currently rather busy with university, so this project is currently on the backburner.**

## Core Ideas

This project leverages three crucial behaviors of Windows Explorer:

1. Double clicking an executable launches that executable with its path as a command-line argument (this is pretty standard behavior for launching executables on all platforms).
2. Dragging a file onto an executable launches that executable with its own path as the first command-line argument, and the path of the file as the second command-line argument.
3. Executables can have icons.

As a result, with some carefully crafted exectuables, we can record whether a tile was double clicked, or whether one tile was dragged to another. The core idea here is to name each executable a unique name which uniquely determines its position among the other tiles. Then, when a tile executable is launched, it can check whether it was supplied 1 or 2 command-line arguments, and from their names, can determined which tile(s) were clicked/dragged.

Furthermore, executables are one of the few file formats in Windows which can contain an icon that will be displayed by Windows Explorer. So, by changing the icons of executables, we can make the tiles in the game appear to change.

The act of a player double-clicking a tile, or dragging one tile to another, is a **player action**. These actions are forwarded to a game server, which decides how to act on them. The game server is the only component that someone who wants to create a game needs to implement.

More details on the overall architecture of the framework are available in `architecture-details.md`.

## Developing WEGAPI Games

Currently, the framework is not in a polished state, and the entire architecture is not entirely fixed. My current focus is getting my own game working first before documenting how to make games with this framework for others. As a result, I am not yet going to document how to develop games with this framework. You are free to dig through the code and figure out what is going on - `ChessServer.java` would be a good place to start.

## Running WEGAPI Games

In the future, I hope to create an installer which would do all the building and setup for you, but it could be a while.

### Disclaimer

**Run games at your own risk. This project attempts to do things with the Windows API that clearly were not intended, and this may have unintended consequences on your machine. For example, during development I have run into issues with the Windows Explorer icon cache, which has resulted in some directories having stale icons which cannot be reliably fixed. If you do work with this project, I suggest creating fresh directories for any games you want to play (i.e. don't use important directories).**

### Dependencies

Clearly, this project only works on Windows 🙂.

This project uses both C++ and Java. As a result, a C++ compiler and Java SDK are required. Familiarity with CMake for the C++ programs is recommended.

### Building

See `src/main/c++/build/README.md` for information on building the C++ executables.

Java code can be built into an Uber jar in the root directory of this repository with:

```shell
mvn clean compile assembly:single
```

### Running

This is going to be high level, because I hope to make this process easier in the future. To be honest, these instructions probably won't make much sense unless you familiarize yourself with the project. My main focus at this point is my own use of the framework, before making it easy to develop with.

Essentially, you need a game directory with the following structure:

```
.gamedata/ (hide so that player cannot see)
├── resources/
│   └── <all icons in .ico format>
├── create_tiles.exe
├── delete_tiles.exe
├── tile.exe
├── wegapi.jar (uber jar compiled above)
└── clang_rt.asan_dbg_dynamic-x86_64.dll
clang_rt.asan_dbg_dynamic-x86_64.dll (hide so that player cannot see)
```

You need to have a running game server on the local machine, and to launch `start_client.exe`, which allows a player to choose the game directory that they will play in. The game should now be playable.

## Attributions

Art for the chess game is taken from [u/Fennyon](https://www.reddit.com/user/Fennyon/) on Reddit, with permission. Specifically, from [this post](https://www.reddit.com/r/PixelArt/comments/rjzw6k/fantasy_chess_pieces/).

## License

This project is licensed under the GNU GPLv3 License - see the LICENSE.md file for details.