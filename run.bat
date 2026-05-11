@echo off
REM =====================================================================
REM NexusBrief - Windows Run Script
REM =====================================================================
REM Before running:
REM   1. Update JAVAFX_PATH below to point to your JavaFX SDK lib folder
REM   2. Ensure lib\ contains: mssql-jdbc-*.jar + all JavaFX jars
REM =====================================================================

REM === EDIT THIS PATH ===
set JAVAFX_PATH=C:\javafx-sdk-26.0.1\lib

echo Compiling NexusBrief...
if not exist out mkdir out

REM Compile all Java sources
dir /s /B src\*.java > sources.txt
javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.graphics,javafx.base -cp "lib\*" -d out @sources.txt
if %errorlevel% neq 0 (
    echo Compilation failed.
    del sources.txt
    pause
    exit /b 1
)
del sources.txt

REM Copy static resources to out\
copy /Y src\dark.css out\dark.css >nul

echo Running NexusBrief...
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.graphics,javafx.base --enable-native-access=javafx.graphics,ALL-UNNAMED -Djava.library.path=lib -cp "out;lib\*" com.nexusbrief.Main

pause
