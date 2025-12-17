#!/usr/bin/env sh
# Proxy script to allow CI to run without the binary wrapper jar
# This script ensures we use the version provided by the environment
if [ -n "$GRADLE_HOME" ]; then
    exec "$GRADLE_HOME/bin/gradle" "$@"
else
    exec gradle "$@"
fi
