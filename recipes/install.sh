#!/bin/bash

RECIPE_LOCATION="https://api.github.com/repos/philcali/device-pool/git/trees/install_targets"

function usage() {
  echo "Usage $(basename $0): [OPTIONS]"
  echo " -l list targets"
  echo " -t install target by name"
  echo " -h displays this help"
  exit 1
}

function obtain_targets() {
  local recipe_url=$(curl -s $RECIPE_LOCATION | jq '.tree[] | select(.path == "recipes") | .url' | tr -d '"')
  local recipes=""
  if [ ! -z "$recipe_url" ]; then
    recipes=$(curl $recipe_url 2>/dev/null | jq '.tree[] | select(.type == "tree") | .path' | tr -d '"')
  fi
  echo "$recipes"
}

function list_targets() {
  echo "Here are the following recipes:"
  for recipe in $(obtain_targets); do
    echo $recipe
  done
  exit 0
}

while getopts "hlt:" flag; do
  case "${flag}" in
    l) list_targets;;
    t) INSTALL_TARGET=${OPTARG};;
    *) usage;;
  esac
done

if [ -z "$INSTALL_TARGET" ]; then
  echo "Use -l to list targets and -t TARGET to install that target."
  usage;
fi

FOUND_TARGET=""
for target in $(obtain_targets); do
  if [ "$target" = "$INSTALL_TARGET" ]; then
    FOUND_TARGET=$target
  fi
done

if [ -z "$FOUND_TARGET" ]; then
  echo "Could not find $INSTALL_TARGET in available targets. Use -l to find targets."
  usage
fi

