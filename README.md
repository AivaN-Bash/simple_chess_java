# Simple Chess

Simple Chess has two separate versions that share the same look:

- **Desktop app:** a Java program for Windows.
- **Web app:** a React game that runs in a browser.

The two versions are separate games. A match started in one version does not appear in the other.

## Play the desktop app

You need a Java Development Kit (JDK) installed. It includes both the compiler that builds the app and Java that runs it. A Java runtime by itself is not enough.

1. Open the project folder.
2. Double-click `run.bat`.

You can also open PowerShell in the project folder and run:

```powershell
.\run.bat
```

The launcher compiles the Java files and opens the game for you. It creates the `out` folder automatically. If you change the code, run `run.bat` again to rebuild and start the latest version.

## Play the web app

The live web game is at [aivan-bash.github.io/simple_chess_java](https://aivan-bash.github.io/simple_chess_java/).

To run the web app on your computer, install [Node.js](https://nodejs.org/) and enter these commands in PowerShell:

```powershell
cd web
npm.cmd install
npm.cmd run dev
```

Open the local address printed by Vite, usually `http://localhost:5173`.

The Java app's **Open web version** button opens the live game. To make it open a different site, set `SIMPLE_CHESS_WEB_URL` or use this JVM option when starting Java:

```powershell
java -DsimpleChess.webUrl=https://your-site.example -cp out Main
```

The JVM option takes priority if both settings are present.

## What you can do

- Play a local two-player match or play against the computer.
- See legal moves, checks, checkmate, stalemate, captured pieces, and material advantage.
- Use castling, en passant, and pawn promotion.
- In a local match, the board turns toward the player whose turn it is. Use **Flip board** on the web to choose a fixed view.
- On the web, drag pieces or select a piece and click a highlighted square.

## Where things live

| Folder or file | What it does |
| --- | --- |
| `run.bat` | Builds and starts the Java desktop app on Windows. |
| `main/Main.java` | Opens the desktop app window. |
| `main/ChessGame.java` | Holds the chess board and enforces the rules. |
| `main/ChessBot.java` | Chooses moves for the desktop computer opponent. |
| `main/ChessBoardPanel.java` | Draws and controls the desktop game. |
| `web/src/App.jsx` | Displays and controls the browser game. |
| `web/src/chess.js` | Holds the browser game's rules and computer move logic. |
| `web/src/PieceIcon.jsx` | Draws matching chess pieces at any screen size. |
| `web/src/bot.worker.js` | Runs browser computer thinking away from the page controls. |
| `web/styles.css` | Sets the browser game's colors, layout, and appearance. |

## Publish the web app

Changes to files in `web/` are built and published to GitHub Pages when pushed to `main`. The project's Pages source should be set to **GitHub Actions** in the repository's **Settings > Pages** page.

To publish with Vercel instead, import the repository and set the project root to `web`, the build command to `npm run build`, and the output folder to `dist`.
