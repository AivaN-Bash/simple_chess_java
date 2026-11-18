export const PIECE_SYMBOLS = {
  wk: "\u2654", wq: "\u2655", wr: "\u2656", wb: "\u2657", wn: "\u2658", wp: "\u2659",
  bk: "\u265A", bq: "\u265B", br: "\u265C", bb: "\u265D", bn: "\u265E", bp: "\u265F"
};

export const PIECE_NAMES = { k: "king", q: "queen", r: "rook", b: "bishop", n: "knight", p: "pawn" };

const PIECE_VALUES = { k: 0, q: 900, r: 500, b: 330, n: 320, p: 100 };
const CAPTURE_ORDER = ["q", "r", "b", "n", "p"];
const KNIGHT_OFFSETS = [[-2,-1],[-2,1],[-1,-2],[-1,2],[1,-2],[1,2],[2,-1],[2,1]];
const BISHOP_DIRECTIONS = [[-1,-1],[-1,1],[1,-1],[1,1]];
const ROOK_DIRECTIONS = [[-1,0],[1,0],[0,-1],[0,1]];

export class ChessGame {
  constructor() {
    this.reset();
  }

  reset() {
    this.board = Array.from({ length: 8 }, () => Array(8).fill(null));
    const backRank = "rnbqkbnr";
    for (let col = 0; col < 8; col++) {
      this.board[0][col] = { type: backRank[col], color: "b", moved: false };
      this.board[1][col] = { type: "p", color: "b", moved: false };
      this.board[6][col] = { type: "p", color: "w", moved: false };
      this.board[7][col] = { type: backRank[col], color: "w", moved: false };
    }
    this.turn = "w";
    this.enPassant = null;
    this.lastMove = null;
    this.capturedBy = { w: [], b: [] };
    this.legalMoveCache = null;
  }

  copy() {
    const copy = Object.create(ChessGame.prototype);
    copy.board = this.board.map(row => row.map(piece => piece && { ...piece }));
    copy.turn = this.turn;
    copy.enPassant = this.enPassant && { ...this.enPassant };
    copy.lastMove = this.lastMove && { ...this.lastMove };
    copy.capturedBy = { w: [...this.capturedBy.w], b: [...this.capturedBy.b] };
    copy.legalMoveCache = this.legalMoveCache;
    return copy;
  }

  pieceAt(row, col) {
    return this.inside(row, col) ? this.board[row][col] : null;
  }

  inside(row, col) {
    return row >= 0 && row < 8 && col >= 0 && col < 8;
  }

  enemy(color) {
    return color === "w" ? "b" : "w";
  }

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
    if (this.legalMoveCache) return this.legalMoveCache;
    const moves = [];
    for (let row = 0; row < 8; row++) {
      for (let col = 0; col < 8; col++) {
        const piece = this.board[row][col];
        if (piece && piece.color === this.turn) moves.push(...this.legalMovesFrom(row, col));
      }
    }
    this.legalMoveCache = moves;
    return moves;
  }

  move(move, promotion = "q") {
    if (!this.legalMovesFrom(move.fr, move.fc).some(candidate => sameMove(candidate, move))) return false;
    this.apply(move, promotion);
    return true;
  }

  apply(move, promotion) {
    this.legalMoveCache = null;
    const piece = this.board[move.fr][move.fc];
    const captured = this.board[move.tr][move.tc]
      || (piece.type === "p" && this.enPassant && move.tr === this.enPassant.row
        && move.tc === this.enPassant.col && move.fc !== move.tc
        ? this.board[move.fr][move.tc]
        : null);
    if (captured) this.capturedBy[piece.color].push(captured.type);

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
      ? { row: (move.tr + move.fr) / 2, col: move.fc }
      : null;
    this.lastMove = { ...move };
    this.turn = this.enemy(this.turn);
  }

  pseudoMoves(row, col) {
    const piece = this.board[row][col];
    const moves = [];
    const add = (toRow, toCol) => {
      if (!this.inside(toRow, toCol)) return false;
      const target = this.board[toRow][toCol];
      if (!target) {
        moves.push({ fr: row, fc: col, tr: toRow, tc: toCol });
        return true;
      }
      if (target.color !== piece.color && target.type !== "k") {
        moves.push({ fr: row, fc: col, tr: toRow, tc: toCol });
      }
      return false;
    };

    if (piece.type === "p") {
      this.addPawnMoves(moves, row, col, piece);
    } else if (piece.type === "n") {
      for (const [rowOffset, colOffset] of KNIGHT_OFFSETS) {
        add(row + rowOffset, col + colOffset);
      }
    } else if (["b", "r", "q"].includes(piece.type)) {
      const directions = piece.type === "b" ? BISHOP_DIRECTIONS
        : piece.type === "r" ? ROOK_DIRECTIONS
          : [...BISHOP_DIRECTIONS, ...ROOK_DIRECTIONS];
      for (const [rowOffset, colOffset] of directions) {
        let toRow = row + rowOffset;
        let toCol = col + colOffset;
        while (this.inside(toRow, toCol)) {
          if (!add(toRow, toCol)) break;
          toRow += rowOffset;
          toCol += colOffset;
        }
      }
    } else if (piece.type === "k") {
      for (let rowOffset = -1; rowOffset <= 1; rowOffset++) {
        for (let colOffset = -1; colOffset <= 1; colOffset++) {
          if (rowOffset || colOffset) add(row + rowOffset, col + colOffset);
        }
      }
      this.addCastlingMoves(moves, row, col, piece);
    }
    return moves;
  }

  addPawnMoves(moves, row, col, pawn) {
    const direction = pawn.color === "w" ? -1 : 1;
    const nextRow = row + direction;
    if (this.inside(nextRow, col) && !this.board[nextRow][col]) {
      moves.push({ fr: row, fc: col, tr: nextRow, tc: col });
      const startRow = pawn.color === "w" ? 6 : 1;
      if (row === startRow && !this.board[row + 2 * direction][col]) {
        moves.push({ fr: row, fc: col, tr: row + 2 * direction, tc: col });
      }
    }

    for (const colOffset of [-1, 1]) {
      const targetCol = col + colOffset;
      if (!this.inside(nextRow, targetCol)) continue;
      const target = this.board[nextRow][targetCol];
      if (target && target.color !== pawn.color && target.type !== "k") {
        moves.push({ fr: row, fc: col, tr: nextRow, tc: targetCol });
      } else if (this.enPassant && this.enPassant.row === nextRow && this.enPassant.col === targetCol) {
        const adjacent = this.board[row][targetCol];
        if (adjacent && adjacent.type === "p" && adjacent.color !== pawn.color) {
          moves.push({ fr: row, fc: col, tr: nextRow, tc: targetCol });
        }
      }
    }
  }

  addCastlingMoves(moves, row, col, king) {
    const enemy = this.enemy(king.color);
    if (king.moved || this.isAttacked(row, col, enemy)) return;

    for (const rookCol of [0, 7]) {
      const rook = this.board[row][rookCol];
      if (!rook || rook.type !== "r" || rook.color !== king.color || rook.moved) continue;
      const step = rookCol < col ? -1 : 1;
      let clear = true;
      for (let file = col + step; file !== rookCol; file += step) {
        if (this.board[row][file]) clear = false;
      }
      const transit = col + step;
      const landing = col + 2 * step;
      if (clear && !this.isAttacked(row, transit, enemy) && !this.isAttacked(row, landing, enemy)) {
        moves.push({ fr: row, fc: col, tr: row, tc: landing });
      }
    }
  }

  inCheck(color) {
    for (let row = 0; row < 8; row++) {
      for (let col = 0; col < 8; col++) {
        const piece = this.board[row][col];
        if (piece && piece.type === "k" && piece.color === color) {
          return this.isAttacked(row, col, this.enemy(color));
        }
      }
    }
    return true;
  }

  isAttacked(row, col, byColor) {
    const pawnRow = row + (byColor === "w" ? 1 : -1);
    for (const colOffset of [-1, 1]) {
      const pawn = this.pieceAt(pawnRow, col + colOffset);
      if (pawn && pawn.color === byColor && pawn.type === "p") return true;
    }
    for (const [rowOffset, colOffset] of KNIGHT_OFFSETS) {
      const piece = this.pieceAt(row + rowOffset, col + colOffset);
      if (piece && piece.color === byColor && piece.type === "n") return true;
    }
    for (let rowOffset = -1; rowOffset <= 1; rowOffset++) {
      for (let colOffset = -1; colOffset <= 1; colOffset++) {
        if (!rowOffset && !colOffset) continue;
        const piece = this.pieceAt(row + rowOffset, col + colOffset);
        if (piece && piece.color === byColor && piece.type === "k") return true;
      }
    }
    return this.rayAttacked(row, col, byColor, BISHOP_DIRECTIONS, "b")
      || this.rayAttacked(row, col, byColor, ROOK_DIRECTIONS, "r");
  }

  rayAttacked(row, col, color, directions, slider) {
    for (const [rowOffset, colOffset] of directions) {
      let toRow = row + rowOffset;
      let toCol = col + colOffset;
      while (this.inside(toRow, toCol)) {
        const piece = this.board[toRow][toCol];
        if (piece) {
          if (piece.color === color && (piece.type === slider || piece.type === "q")) return true;
          break;
        }
        toRow += rowOffset;
        toCol += colOffset;
      }
    }
    return false;
  }
}

export function sameMove(first, second) {
  return first.fr === second.fr && first.fc === second.fc
    && first.tr === second.tr && first.tc === second.tc;
}

export function captureSummary(game, color) {
  const enemy = game.enemy(color);
  const captured = [...game.capturedBy[color]]
    .sort((first, second) => CAPTURE_ORDER.indexOf(first) - CAPTURE_ORDER.indexOf(second));
  const material = types => types.reduce((total, type) => total + PIECE_VALUES[type], 0);

  return {
    pieces: captured.map(type => ({ type, color: enemy })),
    advantage: Math.max(0, Math.round((material(game.capturedBy[color])
      - material(game.capturedBy[enemy])) / 100))
  };
}

export function chooseBotMove(position, thoughtful) {
  const moves = position.legalMoves();
  if (!moves.length) return null;
  const botColor = position.turn;

  if (!thoughtful) {
    const rankedMoves = moves.map(move => {
      const next = position.copy();
      next.apply(move, "q");
      const replies = next.legalMoves();
      const score = !replies.length && next.inCheck(next.turn)
        ? 100_000
        : evaluate(next, botColor);
      return { move, score: score + Math.random() * 12 };
    });
    const bestScore = Math.max(...rankedMoves.map(candidate => candidate.score));
    const bestMoves = rankedMoves.filter(candidate => candidate.score >= bestScore - 12);
    return bestMoves[Math.floor(Math.random() * bestMoves.length)].move;
  }

  let bestScore = -Infinity;
  let bestMoves = [];
  for (const move of moves) {
    const next = position.copy();
    next.apply(move, "q");
    const replies = next.legalMoves();
    const score = replies.length
      ? Math.min(...replies.map(reply => {
        const afterReply = next.copy();
        afterReply.apply(reply, "q");
        const botReplies = afterReply.legalMoves();
        if (!botReplies.length) return afterReply.inCheck(botColor) ? -100_000 : 0;
        return evaluate(afterReply, botColor);
      }))
      : next.inCheck(next.turn) ? 100000 : 0;

    if (score > bestScore) {
      bestScore = score;
      bestMoves = [move];
    } else if (score === bestScore) bestMoves.push(move);
  }
  return bestMoves[Math.floor(Math.random() * bestMoves.length)];
}

function evaluate(game, perspective) {
  let score = 0;
  for (let row = 0; row < 8; row++) {
    for (let col = 0; col < 8; col++) {
      const piece = game.board[row][col];
      if (!piece) continue;
      let value = PIECE_VALUES[piece.type];
      const centerDistance = Math.abs(3 - row) + Math.abs(3 - col);
      if (piece.type === "n" || piece.type === "b") value += (6 - centerDistance) * 4;
      if (piece.type === "p") value += (piece.color === "w" ? 6 - row : row - 1) * 5;
      score += piece.color === perspective ? value : -value;
    }
  }
  if (game.inCheck(perspective)) score -= 24;
  if (game.inCheck(game.enemy(perspective))) score += 24;
  return score;
}
