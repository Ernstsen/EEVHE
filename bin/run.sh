#!/usr/bin/env bash

VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -Ev '(^\[|Download\w+:)')
JAR_PATH="./target/eevhe-${VERSION}.jar"
DEPENDENCIES="./target/dependency-jars/*"

if [[ "$OSTYPE" == "darwin"* ]]; then
  java -cp "${JAR_PATH}:${DEPENDENCIES}" dk.mmj.eevhe.Main "$@"

else
  java -cp "${JAR_PATH};${DEPENDENCIES}" dk.mmj.eevhe.Main "$@"
fi
