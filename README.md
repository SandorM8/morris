# Nine Men's Morris (and Three Men's Morris)

Prepared by: Sándor Máté, PM41PJ

A graphical Java Swing implementation of the classic two-player board game, with a computer opponent.

## Build and run

Requires JDK 17 or newer and Maven.

```
mvn test                         # run the unit tests
mvn package                      # build target/morris-1.0.0.jar
java -jar target/morris-1.0.0.jar
```

## 1. Description of the task

Players form *mills* (three own pieces on a straight line) by placing and then moving pieces.

* **Nine Men's Morris** – three nested squares (24 nodes, 16 mill lines), 9 pieces each. Forming a mill
  captures an opponent piece. A player loses when reduced below 3 pieces (board + hand) or when blocked.
* **Three Men's Morris** – 3x3 board (9 nodes, 8 mill lines: rows, columns and both diagonals), 3 pieces each.
  **There are no captures:** the first player to form a mill wins. A blocked player loses.

Via **New Game → Advanced Setup** the opponent (local player or computer, Easy/Medium/Hard) and the house
rules are chosen. They are locked for the whole game. The state can be saved to / loaded from a
human-readable JSON file. Rendering uses a custom `Graphics2D` component.

### Rules in detail

* White always starts. The computer, when enabled, plays Black.
* **Placing phase:** players alternate placing one piece from their hand on an empty node.
* **Moving phase:** once a player has no pieces in hand, they slide a piece to an adjacent empty node.
* **Mill & removal (9MM):** completing a mill (by placing *or* moving) lets the player remove one opponent
  piece. Pieces in a mill are protected, unless *all* opponent pieces are in mills – then any may be removed.
* **Flying (house rule, 9MM only):** a player with exactly 3 pieces left (none in hand) may jump to any empty
  node. The option is disabled in Three Men's Morris.
* **Game over:** an opponent below 3 pieces (9MM), a mill (3MM), or an opponent with no legal move.

## 2. Use cases

| ID | Use case | Summary |
|----|----------|---------|
| UC-01 | Start a new game with Advanced Setup | Choose mode, opponent, AI difficulty and house rules (undo, flying). Locked for the game. |
| UC-02 | Place / move a piece with the mouse | Placing: click an empty node. Moving: click an own piece, then an adjacent empty node (all empty nodes when flying). A mill switches to the removal phase. |
| UC-03 | Remove a piece after a mill | Click one of the highlighted (red) opponent pieces. |
| UC-04 | Undo | Reverts the previous turn (a placement/move together with its removal). Against the computer it reverts the computer's reply as well, so it is the human's turn again. Only with the *Allow undo* rule. |
| UC-05 | Save and load | Writes/reads the full game state and the active house rules as JSON. The undo history is not saved. |
| UC-06 | Game over | `GameLogic` detects the end; the board stops accepting clicks and a dialog names the winner and offers a new game. |

## 3. Architecture

### 3.1 Technologies

Java 17+, Swing, `Graphics2D`, `java.nio.file.Files` with a hand-written JSON writer and a small parser,
JUnit 5, Maven.

### 3.2 Packages and classes

| Package | Classes |
|---------|---------|
| `morris` | `Main` |
| `morris.model` | enums `Player`, `GameMode`, `GamePhase` (`PLACING, MOVING, FLYING, REMOVING, FINISHED`), `HouseRule`, `OpponentType`, `AIDifficulty`; `Position` (record, node id), `Piece` (record), `Board` (`Map<Position, Piece>`), `GameState` (deep-copyable snapshot), `GameConfig` (immutable settings) |
| `morris.logic` | `BoardTopology` (node coordinates, adjacency, mill lines for both boards), `MillChecker`, `GameLogic` (rules, turns, undo stack), `Turn` (a complete turn), `AIPlayer` |
| `morris.controller` | `GameController` – click handling, piece selection, runs the AI in a `SwingWorker`, save/load/undo |
| `morris.view` | `GameFrame`, `BoardPanel` (`MouseListener`, `paintComponent`), `ControlPanel`, `AdvancedSetupDialog` |
| `morris.service` | `JsonGameManager`, `MiniJson` (parser), `SaveData`, `GameFormatException` |

**Positions.** A position is the index of a node in a fixed topology rather than a free `(row, col)` pair:
9MM uses ids 0–7 (outer ring), 8–15 (middle), 16–23 (inner), each ring clockwise from its top-left corner;
3MM uses `id = row * 3 + col`. `BoardTopology` holds adjacency and mill lines, so `MillChecker`, the AI and
rendering all share one source of truth. Three Men's Morris is simply a second topology.

**Turns.** A turn is one action plus an optional removal. `GameLogic` takes a snapshot at the start of every
turn; that stack is the undo history.

**AI.** Easy: random legal turn. Medium: minimax, depth 2 turns. Hard: minimax, depth 3 turns, both with
alpha-beta pruning. Evaluation = piece difference, open two-in-a-row lines, mobility. The search runs on a
background thread and works on a copy of the game.

### 3.3 Collections

* `Map<Position, Piece>` (`HashMap`) – board contents, O(1) lookup.
* `List<Piece>` (`ArrayList`) – pieces in hand per player (`EnumMap<Player, List<Piece>>`).
* `Stack<GameState>` – undo history.

### 3.4 Save file format

`row`/`col` are the grid coordinates of the node (0–6 for 9MM, 0–2 for 3MM). Only active house rules are
listed. `winner` is `null` unless the game is finished.

```json
{
  "gameMode": "NINE_MENS_MORRIS",
  "opponentType": "AI",
  "aiDifficulty": "MEDIUM",
  "currentTurn": "WHITE",
  "gamePhase": "MOVING",
  "winner": null,
  "board": [
    {"row": 0, "col": 0, "player": "WHITE"},
    {"row": 0, "col": 3, "player": "BLACK"}
  ],
  "whiteHandCount": 0,
  "blackHandCount": 1,
  "activeHouseRules": [
    "UNDO_ENABLED",
    "FLYING_ALLOWED"
  ]
}
```

## 4. Changes from the first version of the specification

* Three Men's Morris has no captures and no minimum piece count; the first mill wins.
* Duplicate "UC-02" renumbered: game over is UC-06.
* Flying only applies at exactly 3 pieces, and only in Nine Men's Morris.
* Undo reverts whole turns and, against the computer, the computer's reply too.
* If every opponent piece is in a mill, any of them may be removed.
* Fixed node-id positions with a topology class instead of free `(row, col)` coordinates.
* Added a `controller` package and a `winner` field in the save file.
