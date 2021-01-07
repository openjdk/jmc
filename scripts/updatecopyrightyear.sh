#!/bin/sh -l

CURRENT_YEAR=$(date +'%Y')

echo "Current year : $CURRENT_YEAR"

# Modified
MODIFIED_FILES=$(git status | grep modified: | awk 'NF>1{print $NF}')

for filesToUpdate in $MODIFIED_FILES
do
    if [[ "$filesToUpdate" == *pom.xml ]] \
    	|| [[ "$filesToUpdate" == *.htm ]] \
    	|| [[ "$filesToUpdate" == *.java ]] \
    	|| [[ "$filesToUpdate" == *.properties ]]
    then
   	sed -i ''  "/Copyright (c)/s/2020/$CURRENT_YEAR/" $filesToUpdate
    	sed -i ''  "/Copyright (c)/s/2018, Oracle/2018, $CURRENT_YEAR, Oracle/" $filesToUpdate
  		echo "Updated : $filesToUpdate"
	else
    		echo "UnSupported files files : $filesToUpdate"
	fi
done
