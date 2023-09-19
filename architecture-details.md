# WEGAPI Architecture Notes

## Core Ideas (Reproduced From README)

This project leverages three crucial behaviors of Windows Explorer:

1. Double clicking an executable launches that executable with its path as a command-line argument (this is pretty standard behavior for launching executables on all platforms).
2. Dragging a file onto an executable launches that executable with its own path as the first command-line argument, and the path of the file as the second command-line argument.
3. Executables can have icons.

As a result, with some carefully crafted exectuables, we can record whether a tile was double clicked, or whether one tile was dragged to another. The core idea here is to name each executable a unique name which uniquely determines its position among the other tiles. Then, when a tile executable is launched, it can check whether it was supplied 1 or 2 command-line arguments, and from their names, can determined which tile(s) were clicked/dragged.

Furthermore, executables are one of the few file formats in Windows which can contain an icon that will be displayed by Windows Explorer. So, by changing the icons of executables, we can make the tiles in the game appear to change.

The act of a player double-clicking a tile, or dragging one tile to another, is a **player action**. These actions are forwarded to a game server, which decides how to act on them. The game server is the only component that someone who wants to create a game needs to implement.

## Details

### Processes

First, a high level overview of the processes that need to be running for a game to occur:

1. There is a daemon that runs on a player's machine (`ClientDaemon.java`), in the background. It is aware of whatever directory the game is being played in. This daemon listens for messages that a certain tile in that directory has been clicked, or that a tile has been dragged onto another tile. There may be multiple daemons running on one machine - for example, if two players are using different directories to play the same game with each other.
2. There is a game server, which in the future may be running on any machine (for now, daemons and the server must run on the same machine). This server listens for messages from player daemons, and is the 'brains' of the game (i.e. it determines what the game state is, and what the effect of player actions are). A game server is essential for multiplayer games as there may be multiple players who each have their own daemon.

### Gameplay

A game is played in a directory, which contain a number of the `tile.exe` executable (compiled from `tile.cpp`). These executables are suitably renamed to enforce a strict ordering - the naming scheme is not particularly important here<sup id="backref1">[1](#footnote1)</sup>. There is a daemon program (`ClientDaemon.java`) that runs and is aware of what this directory is.

When a `tile.exe` executable is launched, it records whether tile(s) were clicked or dragged, and which tiles were involved. It then forwards this information to a Java program (`Client.java`), who itself forwards it to the daemon.

The daemon then contacts the game server, informing it that a player action has occurred. The server processes this action however it likes. It responds to the daemon with a number of instructions that change the visible game to the player (for example, the server may direct the daemon to change the icons of certain tiles, or to display a popup message to the player).

A daemon is required because there needs to be a process listening for these messages from the server. For example, the game may visually change even if the player did not make an action (for example, in chess, you will see the board change when the other player makes a move). `Client.java` routes game actions through `ClientDaemon.java` so that the daemon can control concurrency. For example, the player may have made an action while the game was visually changing - depending on the game, this action may want to be discarded rather than sent to the server<sup id="backref2">[2](#footnote2)</sup>.

****

<b id="footnote1">1</b> For development, an easily debuggable naming scheme is `aaaaa.exe`, `aaaab.exe`, and so on. Eventually, this will be converted to combinations of Unicode whitespace characters so that the player cannot see that a name is even there. Unicode whitespace characters were temporarily enabled to take the example chess image in README.md. [↩](#backref1)

<b id="footnote2">2</b> Currently, this potential concurrency control is not taken advantage of. However, it may be used in the future. [↩](#backref2)
