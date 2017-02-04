#!/usr/bin/env bash
# eg ./mvn-deploy-to-cenral.sh 0.0.2-SNAPSHOT 0.0.3-SNAPSHOT
#-DgenerateBackupPoms=false
mvn versions:set -DnewVersion=$1
mvn versions:commit
mvn deploy
mvn versions:set -DnewVersion=$2
mvn versions:commit

