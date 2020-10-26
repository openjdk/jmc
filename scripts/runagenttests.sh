#!/bin/sh -l

set -e
echo "======== Running agent tests ================"
cd agent
mvn verify
echo "======== Finished ==========================="

