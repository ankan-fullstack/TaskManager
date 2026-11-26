#!/usr/bin/env bash
set -e

# Detect Java 17+ compiler
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC_CMD="$JAVA_HOME/bin/javac"
elif [ -x "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home/bin/javac" ]; then
    JAVAC_CMD="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home/bin/javac"
elif command -v javac >/dev/null 2>&1; then
    JAVAC_CMD="javac"
else
    echo "Error: javac could not be located. Please ensure JDK 17+ is installed."
    exit 1
fi

echo "Compiling Task Manager with $JAVAC_CMD ..."
mkdir -p out
$JAVAC_CMD -d out $(find src -name "*.java")
echo "Compilation successful! Classes placed in out/"
