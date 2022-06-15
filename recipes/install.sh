#!/bin/bash

ASSUME_ROOT=""
HOST_MACHINE=""
DRY_RUN=""
COMMAND_PREFIX=""
RECIPE_LOCATION="https://api.github.com/repos/philcali/device-pool/git/trees/install_targets"
CACHED_RECIPE_CONTENT=""
CACHED_ALL_RECIPES=""

function usage() {
  echo "Usage: $(basename $0) [OPTIONS]"
  echo " -d         performs a dry run; does not install"
  echo " -l         list recipes"
  echo " -t RECIPE  install recipe by name"
  echo " -r         assumes root with sudo"
  echo " -m MACHINE machine in the form of ssh address"
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

function download_blob() {
  local json=$1
  local resource_name=$2
  local resource_url=$(find_blob_url "$json" "$resource_name")
  echo $(curl -s "$resource_url" | jq '.content' | sed 's|\\n||g' | tr -d '"' | base64 -d)
}

function find_blob_url() {
  local json=$1
  local resource_name=$2
  echo "$json" | jq ".tree[] | select(.path == \"$resource_name\") | .url" | tr -d '"'
}

function install_recipe() {
  local recipe_name=$1
  local content=$(echo "$CACHED_ALL_RECIPES" | jq ".tree[] | select(.path == \"$recipe_name\") | .url" | tr -d '"')
  local files=$(curl -s "$content")
  local recipe=$(download_blob "$files" "recipe.json")
  # TODO: Local testing
  # local recipe=$(cat "recipes/$1/recipe.json")

  local old_ifs=$IFS
  IFS=$'\n'
  for command in $(echo "$recipe" | jq '.pre_install.commands[]'); do
    $COMMAND_PREFIX $command
  done

  local output=$(echo "$recipe" | jq '.install.output.file' | tr -d '"')
  for parameter_json in $(echo "$recipe" | jq -c '.install.parameters[] | select(.flags | contains(["required"]))'); do
    local param_name=$(echo "$parameter_json" | jq '.name' | tr -d '"')
    local param_description=$(echo "$parameter_json" | jq '.description' | tr -d '"')
    local param_value=""
    while [ -z "$param_value" ]; do
      read -p "Set the $param_name parameter (context: $param_description): " param_value
    done
    if [ ! -z "$output" ]; then
      $COMMAND_PREFIX "echo $param_name=$param_value >> $output"
    fi
  done

  for file_json in $(echo "$recipe" | jq -c '.install.files[]'); do
    local file_name=$(echo "$file_json" | jq '.name' | tr -d '"')
    local file_dest=$(echo "$file_json" | jq '.destination' | tr -d '"')
    local mod=$(echo "$file_json" | jq '.chmod' | tr -d '"')
    download_blob "$files" "$file_name" > $file_name
    # TODO: Local testing
    # cat "recipes/$1/$file_name" > $file_name
    if [ ! -z "$HOST_MACHINE" ] && [ -z "$DRY_RUN" ]; then
      scp $file_name $HOST_MACHINE:~/
    fi
    $COMMAND_PREFIX mv $file_name $file_dest
    if [ ! -z "$mod" ]; then
      $COMMAND_PREFIX chmod $mod $file_dest
    fi
    rm -f $file_name
  done

  for command in $(echo "$recipe" | jq '.post_install.commands[]' | tr -d '"'); do
    $COMMAND_PREFIX $command
  done

  IFS=$old_ifs
}

function configure_connection() {
  if [ ! -z "$HOST_MACHINE" ]; then
    COMMAND_PREFIX="ssh -o ConnectTimeout=3 $HOST_MACHINE"
  fi

  if [ "$ASSUME_ROOT" = "y" ]; then
    COMMAND_PREFIX="$COMMAND_PREFIX sudo"
  fi

  if [ "$DRY_RUN" = "y" ]; then
    COMMAND_PREFIX="echo $COMMAND_PREFIX"
  fi
}

function find_recipe() {
  local recipe_name=$1
  local FOUND_TARGET=""
  for target in $(obtain_targets); do
    if [ "$target" = "$INSTALL_TARGET" ]; then
      FOUND_TARGET=$target
    fi
  done

  if [ -z "$FOUND_TARGET" ]; then
    echo "Could not find $INSTALL_TARGET in available recipes. Use -l to find recipes."
    usage
  fi
  echo $FOUND_TARGET
}

# Go ahead and reduce subsequent calls to GH
populate_target_cache

while getopts "dhlt:m:r" flag; do
  case "${flag}" in
    l) list_targets;;
    d) DRY_RUN="y";;
    r) ASSUME_ROOT="y";;
    m) HOST_MACHINE=${OPTARG};;
    t) INSTALL_TARGET=${OPTARG};;
    *) usage;;
  esac
done

if [ -z "$INSTALL_TARGET" ]; then
  echo "Use -l to list recipes and -t RECIPE to install that recipe."
  usage;
fi

install_recipe "$(find_recipe "$INSTALL_TARGET")"
echo "Successfully installed $INSTALL_TARGET"