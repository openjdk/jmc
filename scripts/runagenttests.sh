#!/bin/sh -l
set -e

echo "======== Building and testing agent ========="
cd agent
sh -c "mvn ${MAVENPARAMS} verify"
echo "======== Finished ==========================="
