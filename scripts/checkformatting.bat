@echo off
echo "======== Building p2 repo ==================="
cd releng\third-party
call mvn p2:site
echo "======== Starting p2 repo ==================="
start /B cmd /C "mvn jetty:run"
cd ..\..\core
echo "======== Installing core ===================="
call mvn install
echo "======== Running spotless for core =========="
mvn spotless:check
echo "======== Running spotless for application ==="
cd ..
mvn -Puitests spotless:check
echo "======== Finished ==========================="
