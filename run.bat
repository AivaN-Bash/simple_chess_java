@echo off
setlocal

rem Always run from the repository folder, even when this file is double-clicked.
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo Java was not found. Install a Java Development Kit and try again.
    echo The JDK includes both javac and java.
    pause
    exit /b 1
)

if not exist out mkdir out

echo Building Simple Chess...
javac -d out main\ChessGame.java main\ChessBot.java main\ChessBoardPanel.java main\Main.java
if errorlevel 1 (
    echo.
    echo Build failed. Review the compiler message above.
    pause
    exit /b 1
)

echo Starting Simple Chess...
java -cp out Main
