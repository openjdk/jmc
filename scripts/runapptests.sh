#!/bin/sh -l

set -e
# Note that core must have been installed, and the p2 repo started
# before running this.
echo "======== Running application tests =========="
sh -c "mvn verify"
echo "======== Finished ==========================="

