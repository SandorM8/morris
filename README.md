# Major Specification: Nine Men's Morris (Three & Nine Men's Morris)
### Prepared by: Sándor Máté, PM41PJ
## Table of Contents
1. Brief Textual Description of the Task
2. List and Elaboration of Use Cases
    - UC-01: Starting a New Game with Advanced Setup
    - UC-02: Placing a Piece / Executing a Move with the Mouse
    - UC-03: Removing a Piece After Forming a Mill
    - UC-04: Undoing a Move
    - UC-05: Saving and Loading Games (Standard I/O)
    - UC-06: Detecting and Handling Game Over
    - Sketchy Outline of the Solution Idea (Architecture)

3. 
   1. Applied Technologies

   2. Main Classes and Their Roles (Class Design)

   3. Use of the Collection Framework

   4. Planned File Format (JSON Structure)

## 1. Brief Textual Description of the Task

The program is a modern, graphical Java Swing implementation of Nine Men's Morris, a classic board-based strategy game for two players. The goal of the game is for players to form "mills" (three pieces of the same color placed in a straight line) through the logical placement and movement of their pieces. Completing a mill grants a player the right to capture any arbitrary piece belonging to the opponent. The game ends when a player's piece count drops below the permitted minimum, or when an opponent is completely blocked, meaning they are unable to make a valid move.

The software supports two historical game modes:
- Three Men's Morris: A faster, clearer variant played on a smaller 3x3 grid.
 - Nine Men's Morris: The traditional version based on three nested squares, providing a deeper tactical challenge.

 A standout feature of the program is its flexibility: via the Advanced menu option, users can customize opponent parameters at game start (local player or computer AI with various difficulty levels) and house rules, which the program locks in for the duration of the match. The game state can be saved and loaded at any time into a standard, human-readable JSON file. Graphical rendering is handled by a custom low-level drawing component (`Graphics2D`), ensuring smooth mouse control and a responsive game board.
## 2. List and Elaboration of Use Cases
### UC-01: Starting a New Game with Advanced Setup
 - Actor: Player
 - Description: The user selects the "New Game" -> "Advanced Setup" option from the menu. A dialog box appears where game parameters can be locked down:

     - Game Mode: Three Men's Morris or Nine Men's Morris.
     - Opponent Type: Player vs. Player or Player vs. Computer (AI).
     - AI Difficulty Level: (If AI is active) Easy, Medium, or Hard.
     - House Rules: Selectable options (e.g., allow undo, flying rule).

 - Output: The system initializes the game board with the specified settings. Note: These rules and settings cannot be modified during gameplay to prevent mid-game cheating.

### UC-02: Placing a Piece / Executing a Move with the Mouse
 - Actor: Player
 - Description: 
     - Placement Phase: The player clicks on an empty intersection on the board (BoardPanel), and the system places their piece there.
     - Moving Phase: The player clicks on their own piece (selection), and then clicks an adjacent empty space to relocate it.

 - Output: The graphical interface updates immediately (`repaint()`), the piece moves to its new location, and the turn passes to the next player (or AI). If the move results in a mill, the phase shifts to the "removal" phase.

### UC-03: Removing a Piece After Forming a Mill
 - Actor: Player
 - Description: After forming a mill, the player clicks on one of the opponent's selectable pieces (valid targets, meaning pieces not currently part of a mill).

 - Output: The opponent's selected piece is removed from the board and memory, and the game continues.

### UC-04: Undoing a Move
 - Actor: Player
 - Description: The user clicks a button on the control panel to undo their previous move (provided this rule was enabled in the Advanced menu).

 - Output: Using an internal move history stack (`Stack`), the system restores the exact prior board state and player turn.

 ### UC-05: Saving and Loading Games (Standard I/O)

  - Actor: Player
 - Description: Selecting the "Save" or "Load" option from the menu. When saving, the software writes the complete current game state and fixed house rules list into a JSON text file.

 - Output: After loading, the graphical interface and internal data structures accurately reflect the saved snapshot.

 ### UC-02: Placing a Piece / Executing a Move with the Mouse
 - Actor: System / Player
 - Description: The internal logic (GameLogic) checks victory conditions after every move or capture:
     - A player's piece count drops below the allowed minimum (2 for Three Men's Morris, 3 for Nine Men's Morris).
     - A player is blocked (has no valid moves because all adjacent spaces are occupied).

 - Output: The game halts, `BoardPanel` disables mouse clicks, and a pop-up window (`JOptionPane`) declares the winner, offering an option to start a new game.
## 3. Sketchy Outline of the Solution Idea (Architecture)
### 3.1. Applied Technologies
 - Language: Java SE (Java 17 or newer)

 - GUI Framework: Java Swing (JFrame, JPanel, JMenuBar, JMenuItem, JLabel, JButton, JOptionPane)

 - Graphics: Low-level java.awt.Graphics and java.awt.Graphics2D API for drawing board lines and circular pieces inside a custom JPanel (BoardPanel) subclass.

 - File Management: Standard Java I/O (java.io.PrintWriter, java.io.BufferedReader, java.nio.file.Files) processing a hand-assembled JSON structure.

 - Testing: JUnit 5 framework.

### 3.2. Main Classes and Their Roles (Class Design)
 - Main / GameFrame (JFrame subclass): The main window that ties together the menu bar, information panel, and graphical game board.

 - BoardPanel (JPanel subclass): Implements the MouseListener interface. Handles clicks, transforms coordinates, and draws the board and pieces via the paintComponent method (Graphics2D).

 - GameLogic: Contains the game rules, validates moves, checks for mills, phase transitions, and game over states.

 - Board: The logical model of the board. Stores the mapping between positions and pieces in a Map<Position, Piece> collection.

 - Position: A class storing coordinates; will have hashCode() and equals() overridden for proper Map functionality.

 - Piece: A class representing a piece (stores its owner player).

 - GameSession / FileHandler: Responsible for manual JSON serialization and deserialization of the game state.

### 3.3. Use of the Collection Framework
 - Map<Position, Piece> (HashMap): For mapping board positions to pieces with $O(1)$ search time.
 - List<Piece> (ArrayList): Stores pieces in hand that have not yet been placed during the placement phase.
 - Stack<GameState> (Stack): Stores move history to support the Undo feature.

### 3.4. Planned File Format (JSON Structure)
The save file (.json) will be structured and human-readable, with house rules saved as an array listing only active elements:

```JSON
{
  "gameMode": "NINE_MENS_MORRIS",
  "opponentType": "AI",
  "aiDifficulty": "MEDIUM",
  "currentTurn": "WHITE",
  "gamePhase": "MOVING",
  "board": [
    {"row": 0, "col": 0, "player": "WHITE"},
    {"row": 1, "col": 2, "player": "BLACK"}
  ],
  "whiteHandCount": 0,
  "blackHandCount": 1,
  "activeHouseRules": [
    "UNDO_ENABLED",
    "FLYING_ALLOWED"
  ]
}
```

# Classes and Architecture Plan for Implementation

To build a robust, clean, and maintainable object-oriented structure in Java MVC (model, view, controller, and service). Here is a breakdown of the specific classes:
## 1. model Package (Data & Business Entities)
 - Player (Enum):

   - Values: WHITE, BLACK (or helper methods to get the opponent).

 - GameMode (Enum):

   - Values: THREE_MENS_MORRIS, NINE_MENS_MORRIS.

 - GamePhase (Enum):

   - Tracks the current state of a player's turn: PLACING, MOVING, FLYING (if enabled), REMOVING (when a mill is formed), FINISHED.

 - HouseRule (Enum):

   - Values: UNDO_ENABLED, FLYING_ALLOWED.

 - Position (Class):

   - Fields: int row, int col (or ring/index representation depending on your coordinate system).

   - Crucial: Must override equals() and hashCode() to act safely as keys in a HashMap.

 - Piece (Class):

   - Fields: Player owner, String id (optional for tracking).

 - Board (Class):

   - Fields: Map<Position, Piece> grid.

   - Methods: placePiece(Position pos, Piece piece), removePiece(Position pos), Piece getPiece(Position pos), boolean isEmpty(Position pos), initializeBoard(GameMode mode).

 - GameState (Class - for Undo/Snapshot):

   - Captures a snapshot of the board map, hand counts, active player, and game phase so it can be pushed to an undo Stack.

## 2. logic Package (Rules & AI)
 - GameLogic (Class):
   - Manages core rule enforcement.
   - Methods: isValidMove(Position from, Position to), isMill(Position pos, Player player), checkGameOver(), switchTurn().
 - MillChecker (Helper Class / Service):
   - Contains pre-defined winning lines/adjacency matrices for both 3x3 and Nine Men's Morris grids to evaluate whether a placed/moved piece creates a mill.
 - AIPlayer (Class / Interface):
   - Handles computer opponent decisions based on difficulty (Easy: random valid move; Medium/Hard: Minimax algorithm or heuristic blocking of opponent mills).

## 3. view Package (GUI Components - Java Swing)
 - GameFrame (Extends JFrame):
     - The main window container. Manages layouts, menus, and switches between panels (e.g., Main Menu -> Game Board).

 - BoardPanel (Extends JPanel, implements MouseListener):

   - Handles custom rendering (paintComponent with Graphics2D) for lines, nodes, and pieces. Translates pixel mouse clicks into board Position coordinates.

 - ControlPanel (Extends JPanel):

   - Contains buttons for "Undo", "Save", "Load", status labels displaying whose turn it is, and remaining pieces in hand.

 - AdvancedSetupDialog (Extends JDialog):

   - A configuration pop-up window letting the user select Game Mode, Opponent Type, AI Difficulty, and House Rules before starting.

## 4. service Package (I/O & Persistence)
 - JsonGameManager (Class):
   - Handles serializing the active game configurations and board state into a custom JSON string format or reading it back to reconstruct a GameState.