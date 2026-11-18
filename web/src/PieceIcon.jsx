const PIECE_SHAPES = {
  p: (
    <>
      <circle cx="32" cy="12" r="7" />
      <path d="M25 22h14l-2 8 7 13 3 8H17l3-8 7-13-2-8Z" />
      <path d="M17 52h30l3 6H14l3-6Z" />
      <path className="piece-detail" d="M25 31h14M21 48h22" />
    </>
  ),
  r: (
    <>
      <path d="M15 10h8v8h6v-8h6v8h6v-8h8v19h-6l4 22h5v7H12v-7h5l4-22h-6V10Z" />
      <path className="piece-detail" d="M17 25h30M22 31l-3 19h26l-3-19" />
    </>
  ),
  n: (
    <>
      <path d="M15 56c3-7 5-12 3-18-2-5-4-10-2-16l3-9 8 7 2-10 9 9c9 8 13 19 14 36l3 3H14l1-2Z" />
      <path className="piece-detail" d="m20 17 7 6M25 34c4-2 8-2 12 0" />
      <circle className="piece-eye" cx="24" cy="29" r="1.8" />
    </>
  ),
  b: (
    <>
      <circle cx="32" cy="11" r="5" />
      <path d="M32 19c-3 5-10 11-10 19 0 6 4 10 10 12 6-2 10-6 10-12 0-8-7-14-10-19Z" />
      <path d="M22 49h20l5 5H17l5-5ZM14 56h36v5H14z" />
      <path className="piece-detail" d="m36 25-8 16" />
    </>
  ),
  q: (
    <>
      <circle cx="14" cy="14" r="4" />
      <circle cx="26" cy="9" r="4" />
      <circle cx="38" cy="9" r="4" />
      <circle cx="50" cy="14" r="4" />
      <path d="m14 20 5 25h26l5-25-12 12-6-17-6 17-12-12Z" />
      <path d="M19 47h26l4 6H15l4-6ZM13 56h38v5H13z" />
      <path className="piece-detail" d="M22 43h20" />
    </>
  ),
  k: (
    <>
      <path d="M29 7h6v8h8v6h-8v7c8 4 13 11 13 20v3h4v7H12v-7h4v-3c0-9 5-16 13-20v-7h-8v-6h8V7Z" />
      <path className="piece-detail" d="M22 45h20M25 39h14M19 53h26" />
    </>
  )
};

/** Draw chess pieces as inline vectors so every browser uses the same clear shapes. */
export default function PieceIcon({ piece, className = "piece" }) {
  if (!piece || !PIECE_SHAPES[piece.type]) return null;

  return (
    <svg
      aria-hidden="true"
      className={`${className} ${piece.color === "w" ? "white" : "black"}`}
      focusable="false"
      viewBox="0 0 64 64"
    >
      <g
        fill="var(--piece-fill)"
        stroke="var(--piece-stroke)"
        strokeLinejoin="round"
        strokeLinecap="round"
        strokeWidth="2.1"
      >
        {PIECE_SHAPES[piece.type]}
      </g>
    </svg>
  );
}
