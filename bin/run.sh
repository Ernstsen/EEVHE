#!/usr/bin/env bash
JAR_PATH=./target/eevhe-0.0.1-SNAPSHOT.jar
DEPENDENCIES="./target/dependency-jars/*"

java -cp "${JAR_PATH};${DEPENDENCIES}" dk.mmj.eevhe.Main "$@"
