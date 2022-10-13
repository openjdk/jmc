#!/bin/sh -l
set -e

echo "======== Installing and testing core ========"
cd core
sh -c "mvn ${MAVENPARAMS} install"
echo "======== Finished ==========================="
