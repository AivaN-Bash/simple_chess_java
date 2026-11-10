# Simple Chess

A chess app built with Java Swing. The chess rules live in `ChessGame`, which does not depend on Swing. `ChessBoardPanel` displays the board, and `ChessBot` chooses computer moves.

## Run it

From the project folder in PowerShell, compile and start the app:

```powershell
javac -d out main\ChessGame.java main\ChessBot.java main\ChessBoardPanel.java main\main.java
java -cp out Main
```

You need a JDK installed. Drag a piece to a highlighted square, or click a piece and then its destination. In the sidebar, choose two players or play the computer; against the computer, you can choose your color and a casual or thoughtful bot. Use **New game** to restart with those choices. When a pawn reaches the far side, a dialog lets you choose its promotion piece.

The app handles turns, legal moves, check, checkmate, stalemate, castling, en passant, and promotion. It supports local two-player and computer matches; it does not include online play.
