@echo off

echo "======== Running spotless for core =========="
cd core
call mvn %MAVENPARAMS% spotless:check || EXIT /B 5
echo "======== Running spotless for agent ========="
cd ..\agent
call mvn %MAVENPARAMS% spotless:check || EXIT /B 6
echo "======== Running spotless for application ==="
cd ..
call mvn %MAVENPARAMS% -Puitests spotless:check || EXIT /B 7
echo "======== Finished ==========================="
