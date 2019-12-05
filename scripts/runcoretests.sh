#!/bin/sh -l

set -e
echo "======== Running core tests ================="
cd core
mvn verify
echo "======== Finished ==========================="

