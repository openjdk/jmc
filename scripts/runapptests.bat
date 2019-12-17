@echo off
REM Seems we need to restart repo
cd releng\third-party
echo "======== Starting p2 repo ==================="
start /B cmd /C "mvn jetty:run"
cd ..\..\core
REM Seems we need to re-install core
echo "======== Installing core ===================="
call mvn install
cd ..
echo "======== Running application tests =========="
mvn verify
echo "======== Finished ==========================="

