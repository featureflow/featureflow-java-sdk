#!/bin/bash
mvn clean install > build-output.log 2>&1
echo "Build completed, check build-output.log for results"

