@echo off

echo "======== Installing and testing core ========"
cd core
call mvn %MAVENPARAMS% install || EXIT /B 2
echo "======== Finished ==========================="
