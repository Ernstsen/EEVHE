#!/usr/bin/env bash

if [[ $1 =~ ^-?[0-9]+$ ]]
then
    echo "Executing System Configurer"
    ./bin/run.sh --configuration --addresses -1_localhost:8081 -2_localhost:8082 -3_localhost:8083 --outputFolder=conf --time -min=$1
else
    echo "Please supply an integer representing number of minutes for vote"
fi
