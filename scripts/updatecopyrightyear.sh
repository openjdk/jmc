#!/bin/sh -l

START_YEAR=2018
CURRENT_YEAR=$(date +'%Y')
PREVIOUS_YEAR=$(date +%Y | awk -F '/' '{$NF--}1')

echo "Current year : $CURRENT_YEAR"
echo "START_YEAR : $START_YEAR"
echo "PREVIOUS_YEAR : $PREVIOUS_YEAR"

MODIFIED_FILES=$(git status | grep modified: | awk 'NF>1{print $NF}')

for filesToUpdate in $MODIFIED_FILES
do
    if [[ "$filesToUpdate" == *pom.xml ]] \
    	|| [[ "$filesToUpdate" == *.htm ]] \
    	|| [[ "$filesToUpdate" == *.java ]] \
    	|| [[ "$filesToUpdate" == *.properties ]]
    then
      if [ "$(uname)" == "Darwin" ]; then
        # For Mac OS sed inplace doesn't work without these
        sed -i '' '/Copyright (c)/s/$PREVIOUS_YEAR/$CURRENT_YEAR/' $filesToUpdate
        sed -i '' '/Copyright (c)/s/$START_YEAR, Oracle/$START_YEAR, $CURRENT_YEAR, Oracle/' $filesToUpdate
        sed -i '' "/^copyright=/s/$PREVIOUS_YEAR/$CURRENT_YEAR/" $filesToUpdate
      else
        echo "Non Mac"
        sed -i "/Copyright (c)/s/$PREVIOUS_YEAR/$CURRENT_YEAR/" $filesToUpdate
        sed -i "/Copyright (c)/s/$START_YEAR, Oracle/$START_YEAR, $CURRENT_YEAR, Oracle/" $filesToUpdate
        sed -i "/copyright=/s/$PREVIOUS_YEAR/$CURRENT_YEAR/" $fileToupdate
      fi
  		echo "Updated : $filesToUpdate"
	else
    		echo "UnSupported files files : $filesToUpdate"
	fi
done
