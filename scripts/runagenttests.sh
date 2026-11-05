#!/bin/sh
set -e
mvn -v

echo "======== Building and testing agent ========="
cd agent
sh -c "mvn ${MAVENPARAMS} verify"
echo "======== Finished ==========================="
