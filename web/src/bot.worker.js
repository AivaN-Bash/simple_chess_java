import { ChessGame, chooseBotMove } from "./chess.js";

self.onmessage = event => {
  const { position, thoughtful, version } = event.data;

  // Rebuild the engine instance from a structured-clone-safe board snapshot.
  const game = new ChessGame();
  game.board = position.board;
  game.turn = position.turn;
  game.enPassant = position.enPassant;
  game.lastMove = position.lastMove;
  game.legalMoveCache = null;

  const move = chooseBotMove(game, thoughtful);
  self.postMessage({ move, version });
};
