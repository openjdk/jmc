@echo off
echo "======== Running agent tests ================"
cd agent
rem The integration tests fail on windows - change to "mvn verify" once fixed.
mvn test
echo "======== Finished ==========================="

