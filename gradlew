#!/usr/bin/env sh
# Proxy script to allow CI to run without the binary wrapper jar
exec gradle "$@"
