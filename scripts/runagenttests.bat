@echo off

echo "======== Building and testing agent ========="
cd agent
rem The integration tests currently fail on windows - change to "mvn verify" once fixed.
call mvn %MAVENPARAMS% test || EXIT /B 4
echo "======== Finished ==========================="
