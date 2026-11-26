#!/usr/bin/env bash
set -e

# Detect Java 17+ runtime
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif [ -x "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home/bin/java" ]; then
    JAVA_CMD="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_CMD="java"
else
    echo "Error: java could not be located. Please ensure JDK 17+ is installed."
    exit 1
fi

if [ ! -d "out" ] || [ ! -f "out/app/Main.class" ]; then
    ./compile.sh
fi

$JAVA_CMD -cp out app.Main "$@"
