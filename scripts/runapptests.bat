@echo off
echo "======== Installing core ===================="
cd core
call mvn install
cd ..
echo "======== Running application tests =========="
mvn verify
echo "======== Finished ==========================="

