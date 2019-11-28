#!/bin/sh -l

set -e
echo "======== Building p2 repo ==================="
cd releng/third-party
sh -c "mvn p2:site"
sh -c "nohup mvn jetty:run &"
echo "======== Entering core ======================"
cd ../../core
echo "======== Running spotless for core =========="
sh -c "mvn spotless:check"
echo "======== Running spotless for application ==="
cd ..
sh -c "mvn -Puitests spotless:check"
echo "======== Finished ==========================="

