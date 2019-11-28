echo "======== Installing core ===================="
cd core
mvn install
cd ..
echo "======== Running application tests =========="
mvn verify
echo "======== Finished ==========================="

