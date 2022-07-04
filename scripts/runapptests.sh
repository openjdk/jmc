#!/bin/sh -l
set -e

# Note that core must have been installed, and the p2 repo started
# before running this.
echo "======== Building and testing application ==="
sh -c "mvn ${MAVENPARAMS} verify"
echo "======== Finished ==========================="
