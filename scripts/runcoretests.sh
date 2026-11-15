#!/bin/sh
set -e
mvn -v

echo "======== Installing and testing core ========"
cd core
sh -c "mvn ${MAVENPARAMS} install"
echo "======== Finished ==========================="
