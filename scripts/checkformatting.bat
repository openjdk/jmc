@echo off
echo "======== Building p2 repo ==================="
cd releng\third-party
call mvn p2:site || EXIT /B 1
echo "======== Starting p2 repo ==================="
start /B cmd /C "mvn jetty:run"
cd ..\..\core
echo "======== Installing core ===================="
call mvn install || EXIT /B 2
echo "======== Running spotless for core =========="
call mvn spotless:check || EXIT /B 3
cd ..
echo "======== Running spotless for agent =========="
cd agent
call mvn spotless:check || EXIT /B 4
cd ..
echo "======== Running spotless for application ==="
call mvn -Puitests spotless:check || EXIT /B 5
echo "======== Finished ==========================="
