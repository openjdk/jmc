@echo off
REM Seems like the p2 repo needs to be started again...
REM Note that it is built as part of the formatting check, so no need 
REM to rebuild
cd releng\third-party
echo "======== Starting p2 repo ==================="
start /B cmd /C "mvn jetty:run"
cd ..\..
echo "======== Running application tests =========="
mvn verify
echo "======== Finished ==========================="

