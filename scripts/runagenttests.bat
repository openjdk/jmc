@echo off

echo "======== Building and testing agent ========="
cd agent
call mvn %MAVENPARAMS% verify || EXIT /B 4
echo "======== Finished ==========================="
