#!/bin/bash

RECIPE_LOCATION="https://api.github.com/repos/philcali/device-pool/git/trees/install_targets"
CACHED_RECIPE_CONTENT=""
CACHED_ALL_RECIPES=""

function usage() {
  echo "Usage: $(basename $0) [OPTIONS]"
  echo " -l         list recipes"
  echo " -t RECIPE  install recipe by name"
  echo " -h         displays this help"
  exit 1
}

function populate_target_cache() {
  if [ -z "$CACHED_RECIPE_CONTENT" ]; then
      CACHED_RECIPE_CONTENT=$(curl -s "$RECIPE_LOCATION")
  fi
  if [ -z "$CACHED_ALL_RECIPES" ]; then
      local recipe_url=$(echo "$CACHED_RECIPE_CONTENT" | jq '.tree[] | select(.path == "recipes") | .url' | tr -d '"')
      CACHED_ALL_RECIPES=$(curl -s "$recipe_url")
  fi
}

function obtain_targets() {
  local recipe_url=$(echo "$CACHED_RECIPE_CONTENT" | jq '.tree[] | select(.path == "recipes") | .url' | tr -d '"')
  local recipes=$(echo "$CACHED_ALL_RECIPES" | jq '.tree[] | select(.type == "tree") | .path' | tr -d '"')
  echo "$recipes"
}

function list_targets() {
  echo "Here are the following recipes:"
  for recipe in $(obtain_targets); do
    echo $recipe
  done
  exit 0
}

function install_target() {
  local recipe_name=$1
  local content=$(echo "$CACHED_ALL_RECIPES" | jq ".tree[] | select(.path == \"$recipe_name\") | .url" | tr -d '"')
  local files=$(curl -s "$content")
  echo "$files"
}

# Go ahead and reduce subsequent calls to GH
populate_target_cache

while getopts "hlt:" flag; do
  case "${flag}" in
    l) list_targets;;
    t) INSTALL_TARGET=${OPTARG};;
    *) usage;;
  esac
done

if [ -z "$INSTALL_TARGET" ]; then
  echo "Use -l to list recipes and -t RECIPE to install that recipe."
  usage;
fi

FOUND_TARGET=""
for target in $(obtain_targets); do
  if [ "$target" = "$INSTALL_TARGET" ]; then
    FOUND_TARGET=$target
  fi
done

if [ -z "$FOUND_TARGET" ]; then
  echo "Could not find $INSTALL_TARGET in available recipes. Use -l to find recipes."
  usage
fi

install_target "$FOUND_TARGET"