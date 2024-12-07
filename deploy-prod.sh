#!/usr/bin/env bash
openssl aes-256-cbc -d -in codesigning.asc.enc -out codesigning.asc -k ${ENCKEY} -pbkdf2
gpg --fast-import codesigning.asc
./mvnw deploy -P sign,build-extras --settings settings.xml