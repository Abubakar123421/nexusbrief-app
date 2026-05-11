#!/bin/bash
# =====================================================================
# NexusBrief - Linux/Mac Run Script
# =====================================================================
# Before running:
#   1. Update JAVAFX_PATH below to point to your JavaFX SDK lib folder
#   2. Ensure lib/ contains: mssql-jdbc-*.jar + all JavaFX jars
# =====================================================================

# === EDIT THIS PATH ===
JAVAFX_PATH="$HOME/javafx-sdk-17.0.2/lib"

echo "Compiling NexusBrief..."
mkdir -p out

# Compile all Java sources
find src -name "*.java" > sources.txt
javac --module-path "$JAVAFX_PATH" \
      --add-modules javafx.controls,javafx.graphics,javafx.base \
      -cp "lib/*" -d out @sources.txt

if [ $? -ne 0 ]; then
    echo "Compilation failed."
    rm -f sources.txt
    exit 1
fi
rm -f sources.txt

echo "Running NexusBrief..."
java --module-path "$JAVAFX_PATH" \
     --add-modules javafx.controls,javafx.graphics,javafx.base \
     -cp "out:lib/*" com.nexusbrief.Main
