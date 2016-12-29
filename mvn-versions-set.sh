#!/usr/bin/env bash
#-DgenerateBackupPoms=false
mvn versions:set -DnewVersion=$1
mvn versions:commit
