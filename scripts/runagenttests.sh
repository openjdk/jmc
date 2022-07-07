#!/bin/sh -l
set -e

echo "======== Building and testing agent ========="
cd agent
# The integration tests currently fail on windows - change to "mvn verify" once fixed.
if [[ $(uname) == MINGW* ]] || [[ $(uname) == CYGWIN* ]]; then
  sh -c "mvn ${MAVENPARAMS} test"
else
  sh -c "mvn ${MAVENPARAMS} verify"
fi
echo "======== Finished ==========================="
