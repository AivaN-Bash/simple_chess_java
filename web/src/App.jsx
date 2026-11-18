import { useEffect, useMemo, useRef, useState } from "react";
import { captureSummary, ChessGame, PIECE_NAMES } from "./chess.js";
import PieceIcon from "./PieceIcon.jsx";

function moveHint(game, selected, selectedMoves) {
  if (!selected) {
    return "Drag a piece, or select it and choose a highlighted square. To castle, move your king to the rook.";
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

function PlayerCard({ color, active, terminal, name, game }) {
  const captures = captureSummary(game, color);
  const label = color === "w" ? "WHITE" : "BLACK";

  return (
    <div className={`player-card ${active && !terminal ? "active" : ""}`}>
      <PieceIcon className="player-icon" piece={{ type: "k", color }} />
      <span className="player-info">
        <span className="player-heading"><strong>{label}</strong><small>{name}</small></span>
        <span className="capture-tray" aria-label={`${name} captured pieces`} role="group">
          {captures.pieces.length
            ? captures.pieces.map((piece, index) => (
              <span
                aria-hidden="true"
                className="captured-piece"
                key={`${piece.color}${piece.type}-${index}`}
              >
                <PieceIcon className="captured-icon" piece={piece} />
              </span>
            ))
            : <span className="capture-empty">Captured pieces</span>}
          {captures.advantage > 0 && <strong className="material-advantage">+{captures.advantage}</strong>}
        </span>
      </span>
      <span className="turn-dot" aria-hidden="true" />
    </div>
  );
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
  const [boardOrientationOverride, setBoardOrientationOverride] = useState(null);
  const versionRef = useRef(0);
  const timerRef = useRef(null);
  const botWorkerRef = useRef(null);
  const dragOriginRef = useRef(null);

  const botColor = humanColor === "w" ? "b" : "w";
  const isBotGame = opponent === "bot";
  const { checked, terminal } = useMemo(() => {
    const legalMoves = game.legalMoves();
    return { checked: game.inCheck(game.turn), terminal: legalMoves.length === 0 };
  }, [game]);
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

  function ensureBotWorker() {
    if (botWorkerRef.current) return botWorkerRef.current;

    const worker = new Worker(new URL("./bot.worker.js", import.meta.url), { type: "module" });
    worker.onmessage = event => {
      const { move, version } = event.data;
      if (version !== versionRef.current) return;

      const nextGame = gameRef.current.copy();
      if (move && nextGame.move(move, "q")) replaceGame(nextGame);
      timerRef.current = null;
      setBotThinking(false);
      clearSelection();
    };
    worker.onerror = () => {
      worker.terminate();
      botWorkerRef.current = null;
      timerRef.current = null;
      setBotThinking(false);
    };
    botWorkerRef.current = worker;
    return worker;
  }

  useEffect(() => () => {
    versionRef.current += 1;
    if (timerRef.current !== null) window.clearTimeout(timerRef.current);
    botWorkerRef.current?.terminate();
    botWorkerRef.current = null;
  }, []);

  function scheduleBotMove(position, version, opponentMode = opponent, playerColor = humanColor,
      botStyle = difficulty) {
    const computerColor = playerColor === "w" ? "b" : "w";
    if (opponentMode !== "bot" || position.turn !== computerColor || !position.legalMoves().length) return;
    setBotThinking(true);
    timerRef.current = window.setTimeout(() => {
      if (version !== versionRef.current) return;
      ensureBotWorker().postMessage({
        version,
        thoughtful: botStyle === "thoughtful",
        position: {
          board: position.board,
          turn: position.turn,
          enPassant: position.enPassant,
          lastMove: position.lastMove
        }
      });
    }, 120);
  }

  function resetGame(settings = {}) {
    versionRef.current += 1;
    if (timerRef.current !== null) window.clearTimeout(timerRef.current);
    botWorkerRef.current?.terminate();
    botWorkerRef.current = null;
    timerRef.current = null;
    dragOriginRef.current = null;
    setBotThinking(false);
    setPendingPromotion(null);
    setBoardOrientationOverride(null);
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

  function movePieceTo(from, row, col) {
    const current = gameRef.current;
    const movingPiece = current.pieceAt(from.row, from.col);
    if (!movingPiece || movingPiece.color !== current.turn) return false;

    const targetPiece = current.pieceAt(row, col);
    const moves = current.legalMovesFrom(from.row, from.col);
    const destination = movingPiece.type === "k" && targetPiece?.type === "r"
      && targetPiece.color === movingPiece.color && row === from.row
      ? moves.find(move => Math.abs(move.tc - from.col) === 2 && col === (move.tc > from.col ? 7 : 0))
      : moves.find(move => move.tr === row && move.tc === col);
    if (!destination) return false;

    if (movingPiece.type === "p" && (destination.tr === 0 || destination.tr === 7)) {
      setPendingPromotion(destination);
    } else {
      finishMove(destination);
    }
    return true;
  }

  function handleSquare(row, col) {
    const current = gameRef.current;
    if (botThinking || (isBotGame && current.turn === botColor)) return;

    if (selected) {
      if (movePieceTo(selected, row, col)) return;
    }

    const piece = current.pieceAt(row, col);
    if (piece && piece.color === current.turn) {
      setSelected({ row, col });
      setSelectedMoves(current.legalMovesFrom(row, col));
    } else clearSelection();
  }

  function handleDragStart(event, row, col, piece) {
    if (!piece || piece.color !== gameRef.current.turn || botThinking
        || (isBotGame && gameRef.current.turn === botColor)) {
      event.preventDefault();
      return;
    }
    dragOriginRef.current = { row, col };
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", `${row},${col}`);
    setSelected({ row, col });
    setSelectedMoves(gameRef.current.legalMovesFrom(row, col));
  }

  function handleDrop(event, row, col) {
    event.preventDefault();
    const origin = dragOriginRef.current;
    dragOriginRef.current = null;
    if (origin) movePieceTo(origin, row, col);
  }

  const boardFlipped = isBotGame
    ? humanColor === "b"
    : (boardOrientationOverride ?? (game.turn === "b"));
  const squares = [];
  const moveTargets = new Set(selectedMoves.map(move => `${move.tr},${move.tc}`));
  for (let displayRow = 0; displayRow < 8; displayRow++) {
    for (let displayCol = 0; displayCol < 8; displayCol++) {
      const row = boardFlipped ? 7 - displayRow : displayRow;
      const col = boardFlipped ? 7 - displayCol : displayCol;
      const piece = game.pieceAt(row, col);
      const isSelected = selected?.row === row && selected?.col === col;
      const destination = moveTargets.has(`${row},${col}`);
      const movingPiece = selected ? game.pieceAt(selected.row, selected.col) : null;
      const enPassant = destination && movingPiece?.type === "p" && selected.col !== col && !piece;
      const castle = destination && movingPiece?.type === "k" && Math.abs(selected.col - col) === 2;
      const canDrag = piece?.color === game.turn && !botThinking
        && !(isBotGame && game.turn === botColor);
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
          onDragStart={event => handleDragStart(event, row, col, piece)}
          onDragEnd={() => { dragOriginRef.current = null; }}
          onDragOver={event => event.preventDefault()}
          onDrop={event => handleDrop(event, row, col)}
          draggable={Boolean(canDrag)}
          role="gridcell"
          type="button"
        >
          {moveTargets.has(`${row},${col}`) && <span className={piece || enPassant ? "capture-ring" : castle ? "castle-ring" : "move-dot"} />}
          {piece && <PieceIcon piece={piece} />}
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
            <PlayerCard
              active={game.turn === "w"}
              color="w"
              game={game}
              name={whiteName}
              terminal={terminal}
            />
            <PlayerCard
              active={game.turn === "b"}
              color="b"
              game={game}
              name={blackName}
              terminal={terminal}
            />
          </div>

          <section className="status-card" aria-live="polite">
            <p className="section-label">GAME STATUS</p>
            <p className={`status ${terminal ? "over" : checked ? "check" : ""}`}>{status}</p>
          </section>

          <p className="hint-title">HOW TO PLAY</p>
          <p className="hint">{moveHint(game, selected, selectedMoves)}</p>
          <button className="new-game" onClick={resetGame} type="button">New game</button>
          {!isBotGame && <button
            aria-pressed={boardOrientationOverride !== null}
            className="flip-board"
            onClick={() => setBoardOrientationOverride(!boardFlipped)}
            type="button"
          >
            <span aria-hidden="true">&#8635;</span> Flip board
          </button>}
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

      <footer>Local games rotate the board to face the player whose turn it is.</footer>

      {pendingPromotion && <div className="promotion-backdrop">
        <section className="promotion-dialog" role="dialog" aria-modal="true" aria-labelledby="promotion-title">
          <p className="section-label">PAWN PROMOTION</p>
          <h2 id="promotion-title">Choose your piece</h2>
          <div className="promotion-options">
            {[["q", "Queen"], ["r", "Rook"], ["b", "Bishop"], ["n", "Knight"]].map(([type, name]) => (
              <button key={type} type="button" aria-label={name} onClick={() => finishMove(pendingPromotion, type)}>
                <PieceIcon className="promotion-piece" piece={{ type, color: game.turn }} />
              </button>
            ))}
          </div>
        </section>
      </div>}
    </main>
  );
}
