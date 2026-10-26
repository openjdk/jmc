#!/bin/sh
set -e
mvn -v

echo "======== Building p2 repo ==================="
cd releng/third-party
sh -c "mvn ${MAVENPARAMS} p2:site"
echo "======== Starting p2 repo ==================="
sh -c "nohup mvn ${MAVENPARAMS} jetty:run &"
echo "======== Done ==============================="
