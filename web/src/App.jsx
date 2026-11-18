import { useRef, useState } from "react";
import { ChessGame, chooseBotMove, PIECE_NAMES, PIECE_SYMBOLS } from "./chess.js";

function moveHint(game, selected, selectedMoves) {
  if (!selected) {
    return "Select a piece and choose a highlighted square. To castle, select your king, then click the rook.";
  }

  const piece = game.pieceAt(selected.row, selected.col);
  if (piece?.type === "k" && selectedMoves.some(move => Math.abs(move.tc - selected.col) === 2)) {
    return "Castle by clicking your rook, or choose the king's highlighted square.";
  }
  if (piece?.type === "p" && selectedMoves.some(move => move.tc !== selected.col
      && !game.pieceAt(move.tr, move.tc))) {
    return "En passant is available: capture the adjacent pawn on the highlighted square.";
  }
  return "Choose a highlighted square to move.";
}

function squareLabel(row, col, piece) {
  const pieceName = piece ? `${piece.color === "w" ? "White" : "Black"} ${PIECE_NAMES[piece.type]}, ` : "Empty ";
  return `${pieceName}${String.fromCharCode(97 + col)}${8 - row}`;
}

export default function App() {
  const [game, setGame] = useState(() => new ChessGame());
  const gameRef = useRef(game);
  const [selected, setSelected] = useState(null);
  const [selectedMoves, setSelectedMoves] = useState([]);
  const [opponent, setOpponent] = useState("human");
  const [humanColor, setHumanColor] = useState("w");
  const [difficulty, setDifficulty] = useState("casual");
  const [botThinking, setBotThinking] = useState(false);
  const [pendingPromotion, setPendingPromotion] = useState(null);
  const versionRef = useRef(0);
  const timerRef = useRef(null);

  const botColor = humanColor === "w" ? "b" : "w";
  const isBotGame = opponent === "bot";
  const legalMoves = game.legalMoves();
  const checked = game.inCheck(game.turn);
  const terminal = legalMoves.length === 0;
  const activePlayer = game.turn === "w" ? "white" : "black";
  const status = terminal
    ? checked ? `Checkmate — ${game.turn === "w" ? "Black" : "White"} wins` : "Stalemate — draw"
    : botThinking ? "Computer is thinking..."
      : checked ? `${activePlayer === "white" ? "White" : "Black"} is in check`
        : `${activePlayer === "white" ? "White" : "Black"} to move`;

  function replaceGame(nextGame) {
    gameRef.current = nextGame;
    setGame(nextGame);
  }

  function clearSelection() {
    setSelected(null);
    setSelectedMoves([]);
  }

  function scheduleBotMove(position, version, opponentMode = opponent, playerColor = humanColor,
      botStyle = difficulty) {
    const computerColor = playerColor === "w" ? "b" : "w";
    if (opponentMode !== "bot" || position.turn !== computerColor || !position.legalMoves().length) return;
    setBotThinking(true);
    timerRef.current = window.setTimeout(() => {
      if (version !== versionRef.current) return;
      const move = chooseBotMove(position.copy(), botStyle === "thoughtful");
      const current = gameRef.current;
      const nextGame = current.copy();
      if (move && nextGame.turn === computerColor && nextGame.move(move, "q")) replaceGame(nextGame);
      timerRef.current = null;
      setBotThinking(false);
      clearSelection();
    }, 120);
  }

  function resetGame(settings = {}) {
    versionRef.current += 1;
    if (timerRef.current !== null) window.clearTimeout(timerRef.current);
    timerRef.current = null;
    setBotThinking(false);
    setPendingPromotion(null);
    clearSelection();
    const nextGame = new ChessGame();
    replaceGame(nextGame);
    scheduleBotMove(nextGame.copy(), versionRef.current,
      settings.opponent ?? opponent, settings.humanColor ?? humanColor,
      settings.difficulty ?? difficulty);
  }

  function finishMove(move, promotion = "q") {
    const nextGame = gameRef.current.copy();
    if (!nextGame.move(move, promotion)) return;
    replaceGame(nextGame);
    setPendingPromotion(null);
    clearSelection();
    scheduleBotMove(nextGame.copy(), versionRef.current);
  }

  function handleSquare(row, col) {
    const current = gameRef.current;
    if (botThinking || (isBotGame && current.turn === botColor)) return;

    if (selected) {
      const king = current.pieceAt(selected.row, selected.col);
      const clickedPiece = current.pieceAt(row, col);
      if (king?.type === "k" && clickedPiece?.type === "r" && clickedPiece.color === king.color
          && row === selected.row) {
        const castle = selectedMoves.find(move => Math.abs(move.tc - selected.col) === 2
          && col === (move.tc > selected.col ? 7 : 0));
        if (castle) {
          finishMove(castle);
          return;
        }
      }

      const destination = selectedMoves.find(move => move.tr === row && move.tc === col);
      if (destination) {
        const movingPiece = current.pieceAt(selected.row, selected.col);
        if (movingPiece.type === "p" && (row === 0 || row === 7)) {
          setPendingPromotion(destination);
        } else finishMove(destination);
        return;
      }
    }

    const piece = current.pieceAt(row, col);
    if (piece && piece.color === current.turn) {
      setSelected({ row, col });
      setSelectedMoves(current.legalMovesFrom(row, col));
    } else clearSelection();
  }

  const blackAtBottom = isBotGame && humanColor === "b";
  const squares = [];
  for (let displayRow = 0; displayRow < 8; displayRow++) {
    for (let displayCol = 0; displayCol < 8; displayCol++) {
      const row = blackAtBottom ? 7 - displayRow : displayRow;
      const col = blackAtBottom ? 7 - displayCol : displayCol;
      const piece = game.pieceAt(row, col);
      const isSelected = selected?.row === row && selected?.col === col;
      const destination = selectedMoves.some(move => move.tr === row && move.tc === col);
      const movingPiece = selected ? game.pieceAt(selected.row, selected.col) : null;
      const enPassant = destination && movingPiece?.type === "p" && selected.col !== col && !piece;
      const castle = destination && movingPiece?.type === "k" && Math.abs(selected.col - col) === 2;
      const isLastMove = game.lastMove && ((row === game.lastMove.fr && col === game.lastMove.fc)
        || (row === game.lastMove.tr && col === game.lastMove.tc));
      const classes = ["square", (row + col) % 2 === 0 ? "light" : "dark"];
      if (isSelected) classes.push("selected");
      if (isLastMove) classes.push("last-move");

      squares.push(
        <button
          aria-label={squareLabel(row, col, piece)}
          className={classes.join(" ")}
          key={`${row}-${col}`}
          onClick={() => handleSquare(row, col)}
          role="gridcell"
          type="button"
        >
          {destination && <span className={piece || enPassant ? "capture-ring" : castle ? "castle-ring" : "move-dot"} />}
          {piece && <span className={`piece ${piece.color === "w" ? "white" : "black"}`}>
            {PIECE_SYMBOLS[piece.color + piece.type]}
          </span>}
          {displayRow === 7 && <span className="coordinate file">{String.fromCharCode(97 + col)}</span>}
          {displayCol === 0 && <span className="coordinate rank">{8 - row}</span>}
        </button>
      );
    }
  }

  const whiteName = isBotGame ? (botColor === "w" ? "Computer" : "You") : "Player one";
  const blackName = isBotGame ? (botColor === "b" ? "Computer" : "You") : "Player two";

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <span className="brand-mark" aria-hidden="true">♘</span>
          <div>
            <p className="eyebrow">A LITTLE FRIENDLY COMPETITION</p>
            <h1>Simple <span>Chess</span></h1>
          </div>
        </div>
        <p className="top-note">Think ahead. Enjoy the game.</p>
      </header>

      <section className="game-layout" aria-label="Chess game">
        <div className="board-wrap">
          <div id="board" className="board" role="grid" aria-label="Chessboard">
            {squares}
          </div>
        </div>

        <aside className="sidebar">
          <p className="section-label">THE MATCH</p>
          <label className="field-label" htmlFor="opponent">OPPONENT</label>
          <select id="opponent" value={opponent} onChange={event => {
            const nextOpponent = event.target.value;
            setOpponent(nextOpponent);
            resetGame({ opponent: nextOpponent });
          }}>
            <option value="human">Two players</option>
            <option value="bot">Play the computer</option>
          </select>

          {isBotGame && <div className="bot-options">
            <label className="field-label" htmlFor="human-color">YOUR COLOR</label>
            <select id="human-color" value={humanColor} onChange={event => {
              const nextColor = event.target.value;
              setHumanColor(nextColor);
              resetGame({ humanColor: nextColor });
            }}>
              <option value="w">White</option>
              <option value="b">Black</option>
            </select>
            <label className="field-label" htmlFor="difficulty">BOT STYLE</label>
            <select id="difficulty" value={difficulty} onChange={event => {
              const nextDifficulty = event.target.value;
              setDifficulty(nextDifficulty);
              resetGame({ difficulty: nextDifficulty });
            }}>
              <option value="casual">Casual</option>
              <option value="thoughtful">Thoughtful</option>
            </select>
          </div>}

          <div className="players">
            <div className={`player-card ${game.turn === "w" && !terminal ? "active" : ""}`}>
              <span className="player-icon" aria-hidden="true">♔</span>
              <span><strong>WHITE</strong><small>{whiteName}</small></span>
              <span className="turn-dot" aria-hidden="true" />
            </div>
            <div className={`player-card ${game.turn === "b" && !terminal ? "active" : ""}`}>
              <span className="player-icon" aria-hidden="true">♚</span>
              <span><strong>BLACK</strong><small>{blackName}</small></span>
              <span className="turn-dot" aria-hidden="true" />
            </div>
          </div>

          <section className="status-card" aria-live="polite">
            <p className="section-label">GAME STATUS</p>
            <p className={`status ${terminal ? "over" : checked ? "check" : ""}`}>{status}</p>
          </section>

          <p className="hint-title">HOW TO PLAY</p>
          <p className="hint">{moveHint(game, selected, selectedMoves)}</p>
          <button className="new-game" onClick={resetGame} type="button">New game</button>
          <a
            className="desktop-download"
            href="https://github.com/AivaN-Bash/simple_chess_java#java-desktop-app"
            target="_blank"
            rel="noreferrer"
          >
            <span className="desktop-download-icon" aria-hidden="true">&#9816;</span>
            <span className="desktop-download-copy">
              <strong>Get the desktop app</strong>
              <small>Java source and install steps</small>
            </span>
            <span className="desktop-download-arrow" aria-hidden="true">&#8599;</span>
          </a>
        </aside>
      </section>

      <footer>Made for two players at one board — or you and the computer.</footer>

      {pendingPromotion && <div className="promotion-backdrop">
        <section className="promotion-dialog" role="dialog" aria-modal="true" aria-labelledby="promotion-title">
          <p className="section-label">PAWN PROMOTION</p>
          <h2 id="promotion-title">Choose your piece</h2>
          <div className="promotion-options">
            {[["q", "Queen"], ["r", "Rook"], ["b", "Bishop"], ["n", "Knight"]].map(([type, name]) => (
              <button key={type} type="button" aria-label={name} onClick={() => finishMove(pendingPromotion, type)}>
                {PIECE_SYMBOLS[game.turn + type]}
              </button>
            ))}
          </div>
        </section>
      </div>}
    </main>
  );
}
