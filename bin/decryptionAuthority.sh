#!/usr/bin/env bash

echo "Starting Decryption Authority"
if [[ $1 =~ ^-?[0-9]+$ ]]; then
  ./bin/run.sh --authority --conf=conf/common_input.json --port=808"$1" --id="$1"
else
  echo "Please supply an integer id for this instance"
fi
