# Simple Chess

Simple Chess is split into two independent apps that share the same visual style:

- `main/` is the Java Swing desktop game.
- `web/` is the React browser game and deployment project.

Each version runs its own local chess game. The desktop app's **Open web version** button launches the browser version; it does not transfer or synchronize a match between the two apps.

## Java desktop app

From the repository folder, compile and start the desktop app:

```powershell
javac -d out main\ChessGame.java main\ChessBot.java main\ChessBoardPanel.java main\main.java
java -cp out Main
```

The **Open web version** button opens `http://localhost:5173` by default. Start the web dev server first, or set the deployed address when launching Java:

```powershell
java -DsimpleChess.webUrl=https://your-site.vercel.app -cp out Main
```

You can also set `SIMPLE_CHESS_WEB_URL` in the environment. The JVM property takes precedence if both are set.

## React web app

The browser app is a separate Vite project under `web/`. It uses React and keeps the established charcoal, green, ivory, and brass interface.

```powershell
cd web
npm.cmd install
npm.cmd run dev
```

Vite prints the local address after the dev server starts. The desktop button uses port `5173` by default.

Create a production build with:

```powershell
npm.cmd run build
```

The optimized static site is written to `web/dist/`.

## Deploy the web app

### GitHub Pages

The workflow in `.github/workflows/deploy-web.yml` builds and publishes the web app when changes are pushed to `main`. In the GitHub repository, open **Settings → Pages** and choose **GitHub Actions** as the build and deployment source. After the workflow completes, GitHub shows the live URL in its Pages settings and in the workflow deployment environment.

### Vercel

Import this repository in Vercel and set the project root to `web`. Vercel detects Vite; use `npm run build` as the build command and `dist` as the output directory.

After deployment, pass the live URL to the desktop app with `-DsimpleChess.webUrl=https://your-live-site` so its button opens the deployed site.

## Gameplay

Both apps support local two-player and computer matches, legal move highlights, check, checkmate, stalemate, castling, en passant, and pawn promotion. In the browser version, select a piece and click a highlighted square. To castle, select the king and click the rook or the king's highlighted destination.
