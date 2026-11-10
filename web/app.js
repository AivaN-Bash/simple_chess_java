"use strict";

const TYPES = { KING: "k", QUEEN: "q", ROOK: "r", BISHOP: "b", KNIGHT: "n", PAWN: "p" };
const VALUES = { k: 0, q: 900, r: 500, b: 330, n: 320, p: 100 };
const SYMBOLS = {
  wk: "\u2654", wq: "\u2655", wr: "\u2656", wb: "\u2657", wn: "\u2658", wp: "\u2659",
  bk: "\u265A", bq: "\u265B", br: "\u265C", bb: "\u265D", bn: "\u265E", bp: "\u265F"
};
const NAMES = { k: "king", q: "queen", r: "rook", b: "bishop", n: "knight", p: "pawn" };

class ChessGame {
  reset() {
    this.board = Array.from({ length: 8 }, () => Array(8).fill(null));
    const back = "rnbqkbnr";
    for (let col = 0; col < 8; col++) {
      this.board[0][col] = { type: back[col], color: "b", moved: false };
      this.board[1][col] = { type: "p", color: "b", moved: false };
      this.board[6][col] = { type: "p", color: "w", moved: false };
      this.board[7][col] = { type: back[col], color: "w", moved: false };
    }
    this.turn = "w";
    this.enPassant = null;
    this.lastMove = null;
  }

  copy() {
    const copy = Object.create(ChessGame.prototype);
    copy.board = this.board.map(row => row.map(piece => piece && { ...piece }));
    copy.turn = this.turn;
    copy.enPassant = this.enPassant && { ...this.enPassant };
    copy.lastMove = this.lastMove && { ...this.lastMove };
    return copy;
  }

  pieceAt(row, col) { return this.inside(row, col) ? this.board[row][col] : null; }
  inside(row, col) { return row >= 0 && row < 8 && col >= 0 && col < 8; }
  enemy(color) { return color === "w" ? "b" : "w"; }

  legalMovesFrom(row, col) {
    const piece = this.pieceAt(row, col);
    if (!piece || piece.color !== this.turn) return [];
    return this.pseudoMoves(row, col).filter(move => {
      const position = this.copy();
      position.apply(move, "q");
      return !position.inCheck(piece.color);
    });
  }

  legalMoves() {
    const moves = [];
    for (let row = 0; row < 8; row++) {
      for (let col = 0; col < 8; col++) {
        const piece = this.board[row][col];
        if (piece && piece.color === this.turn) moves.push(...this.legalMovesFrom(row, col));
      }
    }
    return moves;
  }

  move(move, promotion = "q") {
    if (!this.legalMovesFrom(move.fr, move.fc).some(candidate => sameMove(candidate, move))) return false;
    this.apply(move, promotion);
    return true;
  }

  apply(move, promotion) {
    const piece = this.board[move.fr][move.fc];
    if (piece.type === "p" && this.enPassant && move.tr === this.enPassant.row
        && move.tc === this.enPassant.col && move.fc !== move.tc && !this.board[move.tr][move.tc]) {
      this.board[move.fr][move.tc] = null;
    }
    this.board[move.fr][move.fc] = null;
    this.board[move.tr][move.tc] = piece;
    if (piece.type === "k" && Math.abs(move.tc - move.fc) === 2) {
      const rookFrom = move.tc > move.fc ? 7 : 0;
      const rookTo = move.tc > move.fc ? move.tc - 1 : move.tc + 1;
      this.board[move.tr][rookTo] = this.board[move.tr][rookFrom];
      this.board[move.tr][rookFrom] = null;
      this.board[move.tr][rookTo].moved = true;
    }
    piece.moved = true;
    if (piece.type === "p" && (move.tr === 0 || move.tr === 7)) {
      this.board[move.tr][move.tc] = { type: promotion, color: piece.color, moved: true };
    }
    this.enPassant = piece.type === "p" && Math.abs(move.tr - move.fr) === 2
      ? { row: (move.tr + move.fr) / 2, col: move.fc } : null;
    this.lastMove = { ...move };
    this.turn = this.enemy(this.turn);
  }

  pseudoMoves(row, col) {
    const piece = this.board[row][col];
    const moves = [];
    const add = (tr, tc) => {
      if (!this.inside(tr, tc)) return false;
      const target = this.board[tr][tc];
      if (!target) { moves.push({ fr: row, fc: col, tr, tc }); return true; }
      if (target.color !== piece.color && target.type !== "k") moves.push({ fr: row, fc: col, tr, tc });
      return false;
    };

    if (piece.type === "p") {
      const direction = piece.color === "w" ? -1 : 1;
      const next = row + direction;
      if (this.inside(next, col) && !this.board[next][col]) {
        add(next, col);
        const start = piece.color === "w" ? 6 : 1;
        if (row === start && !this.board[row + 2 * direction][col]) add(row + 2 * direction, col);
      }
      for (const dc of [-1, 1]) {
        const tc = col + dc;
        if (!this.inside(next, tc)) continue;
        const target = this.board[next][tc];
        if (target && target.color !== piece.color && target.type !== "k") add(next, tc);
        else if (this.enPassant && this.enPassant.row === next && this.enPassant.col === tc) {
          const adjacent = this.board[row][tc];
          if (adjacent && adjacent.type === "p" && adjacent.color !== piece.color) add(next, tc);
        }
      }
    } else if (piece.type === "n") {
      for (const [dr, dc] of [[-2,-1],[-2,1],[-1,-2],[-1,2],[1,-2],[1,2],[2,-1],[2,1]]) add(row + dr, col + dc);
    } else if (["b", "r", "q"].includes(piece.type)) {
      const directions = [];
      if (["b", "q"].includes(piece.type)) directions.push([-1,-1],[-1,1],[1,-1],[1,1]);
      if (["r", "q"].includes(piece.type)) directions.push([-1,0],[1,0],[0,-1],[0,1]);
      for (const [dr, dc] of directions) {
        let tr = row + dr, tc = col + dc;
        while (this.inside(tr, tc)) {
          const clear = add(tr, tc);
          if (!clear) break;
          tr += dr; tc += dc;
        }
      }
    } else if (piece.type === "k") {
      for (let dr = -1; dr <= 1; dr++) for (let dc = -1; dc <= 1; dc++) {
        if (dr || dc) add(row + dr, col + dc);
      }
      if (!piece.moved && !this.isAttacked(row, col, this.enemy(piece.color))) {
        for (const rookCol of [0, 7]) {
          const rook = this.board[row][rookCol];
          if (!rook || rook.type !== "r" || rook.color !== piece.color || rook.moved) continue;
          const step = rookCol < col ? -1 : 1;
          let clear = true;
          for (let c = col + step; c !== rookCol; c += step) if (this.board[row][c]) clear = false;
          const transit = col + step, landing = col + 2 * step, enemy = this.enemy(piece.color);
          if (clear && !this.isAttacked(row, transit, enemy) && !this.isAttacked(row, landing, enemy)) add(row, landing);
        }
      }
    }
    return moves;
  }

  inCheck(color) {
    for (let row = 0; row < 8; row++) for (let col = 0; col < 8; col++) {
      const piece = this.board[row][col];
      if (piece && piece.type === "k" && piece.color === color) return this.isAttacked(row, col, this.enemy(color));
    }
    return true;
  }

  isAttacked(row, col, byColor) {
    const pawnRow = row + (byColor === "w" ? 1 : -1);
    for (const dc of [-1, 1]) {
      const pawn = this.pieceAt(pawnRow, col + dc);
      if (pawn && pawn.color === byColor && pawn.type === "p") return true;
    }
    for (const [dr, dc] of [[-2,-1],[-2,1],[-1,-2],[-1,2],[1,-2],[1,2],[2,-1],[2,1]]) {
      const piece = this.pieceAt(row + dr, col + dc);
      if (piece && piece.color === byColor && piece.type === "n") return true;
    }
    for (let dr = -1; dr <= 1; dr++) for (let dc = -1; dc <= 1; dc++) {
      if (dr || dc) {
        const piece = this.pieceAt(row + dr, col + dc);
        if (piece && piece.color === byColor && piece.type === "k") return true;
      }
    }
    return this.rayAttacked(row, col, byColor, [[-1,-1],[-1,1],[1,-1],[1,1]], "b")
      || this.rayAttacked(row, col, byColor, [[-1,0],[1,0],[0,-1],[0,1]], "r");
  }

  rayAttacked(row, col, color, directions, slider) {
    for (const [dr, dc] of directions) {
      let r = row + dr, c = col + dc;
      while (this.inside(r, c)) {
        const piece = this.board[r][c];
        if (piece) {
          if (piece.color === color && (piece.type === slider || piece.type === "q")) return true;
          break;
        }
        r += dr; c += dc;
      }
    }
    return false;
  }
}

function sameMove(a, b) { return a.fr === b.fr && a.fc === b.fc && a.tr === b.tr && a.tc === b.tc; }

function chooseBotMove(position, thoughtful) {
  const moves = position.legalMoves();
  if (!moves.length) return null;
  if (!thoughtful) return moves[Math.floor(Math.random() * moves.length)];
  const bot = position.turn;
  let best = -Infinity, choices = [];
  for (const move of moves) {
    const next = position.copy(); next.apply(move, "q");
    const replies = next.legalMoves();
    let score;
    if (!replies.length) score = next.inCheck(next.turn) ? 100000 : 0;
    else score = Math.min(...replies.map(reply => {
      const afterReply = next.copy(); afterReply.apply(reply, "q");
      return evaluate(afterReply, bot);
    }));
    if (score > best) { best = score; choices = [move]; }
    else if (score === best) choices.push(move);
  }
  return choices[Math.floor(Math.random() * choices.length)];
}

function evaluate(game, perspective) {
  let score = 0;
  for (let row = 0; row < 8; row++) for (let col = 0; col < 8; col++) {
    const piece = game.board[row][col];
    if (!piece) continue;
    let value = VALUES[piece.type];
    const centerDistance = Math.abs(3 - row) + Math.abs(3 - col);
    if (piece.type === "n" || piece.type === "b") value += (6 - centerDistance) * 4;
    if (piece.type === "p") value += (piece.color === "w" ? 6 - row : row - 1) * 5;
    score += piece.color === perspective ? value : -value;
  }
  if (game.inCheck(perspective)) score -= 24;
  if (game.inCheck(perspective === "w" ? "b" : "w")) score += 24;
  return score;
}

const boardElement = document.querySelector("#board");
const statusElement = document.querySelector("#status");
const opponentSelect = document.querySelector("#opponent");
const humanColorSelect = document.querySelector("#human-color");
const difficultySelect = document.querySelector("#difficulty");
const botOptions = document.querySelector("#bot-options");
const promotionDialog = document.querySelector("#promotion-dialog");
let game = new ChessGame();
let selected = null;
let selectedMoves = [];
let botThinking = false;
let pendingPromotion = null;
let botTimer = null;
let gameVersion = 0;

function isBotGame() { return opponentSelect.value === "bot"; }
function botColor() { return humanColorSelect.value === "w" ? "b" : "w"; }
function isHumanTurn() { return !isBotGame() || game.turn !== botColor(); }
function displayToBoard(row, col) {
  return isBotGame() && humanColorSelect.value === "b" ? [7 - row, 7 - col] : [row, col];
}

function render() {
  boardElement.replaceChildren();
  const blackAtBottom = isBotGame() && humanColorSelect.value === "b";
  for (let displayRow = 0; displayRow < 8; displayRow++) {
    for (let displayCol = 0; displayCol < 8; displayCol++) {
      const [row, col] = displayToBoard(displayRow, displayCol);
      const piece = game.pieceAt(row, col);
      const square = document.createElement("button");
      square.type = "button";
      square.className = `square ${(row + col) % 2 === 0 ? "light" : "dark"}`;
      square.setAttribute("role", "gridcell");
      square.setAttribute("aria-label", squareLabel(row, col, piece));
      if (game.lastMove && ((row === game.lastMove.fr && col === game.lastMove.fc)
          || (row === game.lastMove.tr && col === game.lastMove.tc))) square.classList.add("last-move");
      if (selected && row === selected.row && col === selected.col) square.classList.add("selected");
      if (selectedMoves.some(move => move.tr === row && move.tc === col)) {
        const hint = document.createElement("span");
        hint.className = game.pieceAt(row, col) ? "capture-ring" : "move-dot";
        square.append(hint);
      }
      if (piece) {
        const glyph = document.createElement("span");
        glyph.className = `piece ${piece.color === "w" ? "white" : "black"}`;
        glyph.textContent = SYMBOLS[piece.color + piece.type];
        square.append(glyph);
      }
      if ((!blackAtBottom && displayRow === 7) || (blackAtBottom && displayRow === 7)) {
        const file = document.createElement("span"); file.className = "coordinate file";
        file.textContent = String.fromCharCode(97 + col); square.append(file);
      }
      if (displayCol === 0) {
        const rank = document.createElement("span"); rank.className = "coordinate rank";
        rank.textContent = String(8 - row); square.append(rank);
      }
      square.addEventListener("click", () => handleSquare(row, col));
      boardElement.append(square);
    }
  }
  updateSidebar();
}

function squareLabel(row, col, piece) {
  const name = piece ? `${piece.color === "w" ? "White" : "Black"} ${NAMES[piece.type]}, ` : "Empty ";
  return `${name}${String.fromCharCode(97 + col)}${8 - row}`;
}

function handleSquare(row, col) {
  if (!isHumanTurn() || botThinking) return;
  if (selected && selectedMoves.some(move => move.tr === row && move.tc === col)) {
    const piece = game.pieceAt(selected.row, selected.col);
    const move = { fr: selected.row, fc: selected.col, tr: row, tc: col };
    if (piece.type === "p" && (row === 0 || row === 7)) {
      pendingPromotion = move;
      promotionDialog.showModal();
    } else finishMove(move);
    return;
  }
  const piece = game.pieceAt(row, col);
  if (piece && piece.color === game.turn) {
    selected = { row, col };
    selectedMoves = game.legalMovesFrom(row, col);
  } else {
    selected = null;
    selectedMoves = [];
  }
  render();
}

promotionDialog.addEventListener("close", () => {
  if (!pendingPromotion) return;
  const move = pendingPromotion;
  const choice = promotionDialog.returnValue || "q";
  pendingPromotion = null;
  finishMove(move, choice);
});

function finishMove(move, promotion = "q") {
  if (!game.move(move, promotion)) return;
  selected = null;
  selectedMoves = [];
  render();
  if (isBotGame() && game.turn === botColor() && game.legalMoves().length) {
    botThinking = true;
    updateSidebar();
    const botPosition = game.copy();
    const version = gameVersion;
    botTimer = window.setTimeout(() => {
      if (version !== gameVersion) return;
      const choice = chooseBotMove(botPosition, difficultySelect.value === "thoughtful");
      if (choice && game.turn === botColor()) game.move(choice, "q");
      botThinking = false;
      botTimer = null;
      render();
    }, 120);
  }
}

function updateSidebar() {
  const isTerminal = game.legalMoves().length === 0;
  const checked = game.inCheck(game.turn);
  const sideName = game.turn === "w" ? "White" : "Black";
  statusElement.className = "status";
  if (isTerminal && checked) {
    statusElement.textContent = `Checkmate — ${game.turn === "w" ? "Black" : "White"} wins`;
    statusElement.classList.add("over");
  } else if (isTerminal) {
    statusElement.textContent = "Stalemate — draw";
    statusElement.classList.add("over");
  } else if (botThinking) statusElement.textContent = "Computer is thinking...";
  else if (checked) {
    statusElement.textContent = `${sideName} is in check`;
    statusElement.classList.add("check");
  } else statusElement.textContent = `${sideName} to move`;

  document.querySelector("#white-player").classList.toggle("active", game.turn === "w" && !isTerminal);
  document.querySelector("#black-player").classList.toggle("active", game.turn === "b" && !isTerminal);
  document.querySelector("#white-name").textContent = isBotGame() ? (botColor() === "w" ? "Computer" : "You") : "Player one";
  document.querySelector("#black-name").textContent = isBotGame() ? (botColor() === "b" ? "Computer" : "You") : "Player two";
}

function startNewGame() {
  gameVersion++;
  if (botTimer !== null) window.clearTimeout(botTimer);
  botTimer = null;
  game.reset();
  selected = null;
  selectedMoves = [];
  botThinking = false;
  render();
  if (isBotGame() && game.turn === botColor()) {
    botThinking = true;
    updateSidebar();
    const position = game.copy();
    const version = gameVersion;
    botTimer = window.setTimeout(() => {
      if (version !== gameVersion) return;
      const move = chooseBotMove(position, difficultySelect.value === "thoughtful");
      if (move && game.turn === botColor()) game.move(move, "q");
      botThinking = false;
      botTimer = null;
      render();
    }, 120);
  }
}

opponentSelect.addEventListener("change", () => {
  botOptions.hidden = !isBotGame();
  startNewGame();
});
humanColorSelect.addEventListener("change", startNewGame);
difficultySelect.addEventListener("change", startNewGame);
document.querySelector("#new-game").addEventListener("click", startNewGame);

render();
