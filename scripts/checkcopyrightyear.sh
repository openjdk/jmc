#!/usr/bin/env bash

# set remote for upstream repository
git remote -v | grep -w upstream || git remote add upstream https://github.com/openjdk/jmc.git
git fetch upstream

CURRENT_YEAR=$(date +'%Y')
MODIFIED_FILES=$(git diff --name-only upstream/master)
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
    # check if the PR branch is up-to-date with upstream/master
    # borrowed from: https://stackoverflow.com/a/39402294
    if git merge-base --is-ancestor upstream/master @
    then
        echo "Branch is up-to-date."
    else
        echo "Branch is out of date with upstream/master. Please rebase your branch and try again."
        exit 1
    fi
    echo "There is a total of $counter copyright year(s) that require updating."
    exit 1
else
    exit 0
fi
