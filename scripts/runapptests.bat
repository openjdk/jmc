@echo off

rem Note that core must have been installed, and the p2 repo started
rem before running this.
echo "======== Building and testing application ==="
call mvn %MAVENPARAMS% verify || EXIT /B 3
echo "======== Finished ==========================="
