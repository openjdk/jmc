#!/usr/bin/env bash

CURRENT_YEAR=$(date +'%Y')
MODIFIED_FILES=$(git diff --name-only origin/master)
counter=0

for fileToCheck in $MODIFIED_FILES
do
    if [[ ($fileToCheck =~ .*\.java) || ($fileToCheck =~ .*\.htm) || ($fileToCheck =~ pom.xml) || ($fileToCheck =~ .*\.properties) ]]
    then
        LATEST=$(sed -n "s/^.*Copyright (c).\+\(20[[:digit:]]\{2\}\).*$/\1/p" $fileToCheck)
        for year in $LATEST; do
            if [ $year -ne $CURRENT_YEAR ]
            then
                counter=$((counter + 1))
                echo "Requires update: $fileToCheck"
            fi
        done
    fi
done
if [ $counter -ne 0 ]
then
    echo "There is a total of $counter copyright year(s) that require updating."
    exit 1
else
    exit 0
fi
