#!/usr/bin/env bash
#
# Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
# Copyright (c) 2023, 2026, Red Hat Inc. All rights reserved.
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The contents of this file are subject to the terms of either the Universal Permissive License
# v 1.0 as shown at https://oss.oracle.com/licenses/upl
#
# or the following license:
#
# Redistribution and use in source and binary forms, with or without modification, are permitted
# provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice, this list of conditions
# and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright notice, this list of
# conditions and the following disclaimer in the documentation and/or other materials provided with
# the distribution.
#
# 3. Neither the name of the copyright holder nor the names of its contributors may be used to
# endorse or promote products derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
# IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
# FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
# WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

UPSTREAM_URL=${1:-https://github.com/openjdk/jmc.git}
UPSTREAM_BRANCH=${2:-master}

# set remote for upstream repository
git remote -v | grep -w upstream || git remote add upstream "$UPSTREAM_URL"
git fetch upstream

CURRENT_YEAR=$(date +'%Y')
COMMITTED_FILES=$(git diff --name-only --diff-filter=d "upstream/${UPSTREAM_BRANCH}...HEAD")
UNCOMMITTED_FILES=$(git diff --name-only --diff-filter=d)
MODIFIED_FILES=$(echo -e "$COMMITTED_FILES\n$UNCOMMITTED_FILES" | sort -u | grep -v '^$')
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
    if git merge-base --is-ancestor "upstream/${UPSTREAM_BRANCH}" @
    then
        echo "Branch is up-to-date."
    else
        echo "Branch is out of date with upstream/${UPSTREAM_BRANCH}. Please rebase your branch and try again."
        exit 1
    fi
    echo "There is a total of $counter copyright year(s) that require updating."
    exit 1
else
    exit 0
fi
