@echo off

echo "======== Building p2 repo ==================="
cd releng\third-party
call mvn %MAVENPARAMS% p2:site || EXIT /B 1
echo "======== Starting p2 repo ==================="
start /B cmd /C "mvn %MAVENPARAMS% jetty:run"
echo "======== Done ==============================="
