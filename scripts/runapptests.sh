#!/bin/sh -l

set -e
echo "======== Installing core ===================="
cd core
sh -c "mvn install"
cd ..
echo "======== Running application tests =========="
sh -c "mvn verify"
echo "======== Finished ==========================="

