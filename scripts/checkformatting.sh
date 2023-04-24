#!/bin/sh -l
set -e

echo "======== Running spotless for core =========="
cd core
sh -c "mvn ${MAVENPARAMS} spotless:check"
echo "======== Running spotless for agent ========="
cd ../agent
sh -c "mvn ${MAVENPARAMS} spotless:check"
echo "======== Running spotless for application ==="
cd ..
sh -c "mvn ${MAVENPARAMS} -Puitests spotless:check"
echo "======== Finished ==========================="
