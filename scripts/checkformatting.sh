#!/bin/sh -l

set -e
echo "======== Building p2 repo ==================="
cd releng/third-party
sh -c "mvn p2:site"
echo "======== Starting p2 repo ==================="
sh -c "nohup mvn jetty:run &"
cd ../..
echo "======== Installing core ===================="
cd core
sh -c "mvn install"
echo "======== Running spotless for core =========="
sh -c "mvn spotless:check"
cd ..
echo "======== Running spotless for agent ========="
cd agent
sh -c "mvn spotless:check"
cd ..
echo "======== Running spotless for application ==="
sh -c "mvn -Puitests spotless:check"
echo "======== Finished ==========================="

