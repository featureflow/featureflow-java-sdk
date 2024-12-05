#!/usr/bin/env bash
echo Deploy to Production
openssl aes-256-cbc -d -in codesigning.asc.enc -out codesigning.asc -k $ENCKEY
gpg --fast-import codesigning.asc
mvn deploy -P sign,build-extras --settings settings.xml