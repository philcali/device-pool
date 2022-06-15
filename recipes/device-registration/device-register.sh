#!/bin/bash
#
# Copyright (c) 2022 Philip Cali
# Released under Apache-2.0 License
#     (https://www.apache.org/licenses/LICENSE-2.0)
#


function usage() {
  echo "Usage: $(basename $0) [OPTIONS]"
  echo " -r ROLE_ALIAS           The role alias to assume"
  echo " -e CREDENTIALS_ENDPOINT The credentials endpoint to use"
  echo " -n THING_NAME           The name of the AWS IoT Thing"
  echo " -c THING_CERT           The path to the X509 certificate file"
  echo " -k THING_KEY            The path to the private key"
  echo " -a CA_CERT              The path to the CA certificate file"
  echo " -l ENDPOINT             The endpoint for the device lab (required)"
  echo " -P PUBLIC_ADDRESS       The publicAddress field (default $(__default_public_address))"
  echo " -A PRIVATE_ADDRESS      The privateAddress field (default none)"
  echo " -p POOL_ID              The device pool ID to associate to (required)"
  echo " -i DEVICE_ID            The device ID to use (default $(__default_device_id))"
  echo " -x EXPIRES_IN           The device expires in field (default +2 hour)"
  exit 1
}

function __validate() {
  for parameter in ROLE_ALIAS THING_NAME THING_CERT THING_KEY CA_CERT; do
    if [ -z "${!parameter}" ]; then
      echo "The $parameter is required:"
      usage
    fi
  done
}

function __default_public_address() {
  local device_for_default_gateway=$(ip route | grep default | sed -E 's|.+ dev ([^\s]+) p.+|\1|g')
  echo $(ip route | grep -v default | grep $device_for_default_gateway | sed -E 's|.+ link src ([^\s]+) m.+|\1|')
}

function __default_device_id() {
  echo $(hostname)
}

function __inject_device_properties() {
  if [ ! -z "$PRIVATE_ADDRESS" ]; then
    sed -i "s|{|{\"privateAddress\":\"$PRIVATE_ADDRESS\",|" request.json
  fi
}

while getopts "hr:e:n:c:a:k:l:P:A:p:i:x:" flag
do
  case "${flag}" in
    r) ROLE_ALIAS="${OPTARG}";;
    e) CREDENTIALS_ENDPOINT="${OPTARG}";;
    n) THING_NAME="${OPTARG}";;
    c) THING_CERT="${OPTARG}";;
    k) THING_KEY="${OPTARG}";;
    a) CA_CERT="${OPTARG}";;
    l) ENDPOINT="${OPTARG}";;
    p) POOL_ID="${OPTARG}";;
    P) PUBLIC_ADDRESS="${OPTARG}";;
    A) PRIVATE_ADDRESS="${OPTARG}";;
    i) DEVICE_ID="${OPTARG}";;
    x) EXPIRES_IN="${OPTARG}";;
    *) usage;;
  esac
done

if [ -z "$ENDPOINT" ] || [ -z "$POOL_ID" ]; then
  echo "The ENDPOINT and POOL_ID are required parameters."
  usage
fi

if [ ! -z "$CREDENTIALS_ENDPOINT" ]; then
  __validate
  credentials=$(curl \
      --cert $THING_CERT --key $THING_KEY --cacert $CA_CERT \
      -H "x-amzn-iot-thingname: $THING_NAME" https://$CREDENTIALS_ENDPOINT/role-aliases/$ROLE_ALIAS/credentials)
  if [ $(echo $?) -ne 0 ]; then
    echo "Failed to acquire credentials, please examine the error and try again: $credentials"
    exit 1
  fi
  AWS_ACCESS_KEY_ID=$(echo "$credentials" | jq '.credentials.accessKeyId')
  AWS_SECRET_ACCESS_KEY=$(echo "$credentials" | jq '.credentials.secretAccessKey')
  AWS_SECURITY_TOKEN=$(echo "$credentials" | jq '.credentials.sessionToken')
fi

DEFAULT_DEVICE_ID=$(__default_device_id)
DEFAULT_PUBLIC_ADDRESS=$(__default_public_address)
DEFAULT_EXPIRES_IN=$(date -u -d '+2 hour' '+%FT%TZ')
DEVICE_ID=${DEVICE_ID:-$DEFAULT_DEVICE_ID}
PUBLIC_ADDRESS=${PUBLIC_ADDRESS:-$DEFAULT_PUBLIC_ADDRESS}
EXPIRES_IN=${EXPIRES_IN:-DEFAULT_EXPIRES_IN}

TEMP_DIR=$(mktemp -d)
previous_device=$(awscurl --service execute-api -X GET -H "Accept: application/json" $ENDPOINT/pools/$POOL_ID/devices/$DEVICE_ID 2>/dev/null)
if [ $(echo $?) -eq 0 ]; then
  cat > $TEMP_DIR/request.json << EOL
{
  "publicAddress": "$PUBLIC_ADDRESS",
  "expiresIn": "$EXPIRES_IN"
}
EOL
  __inject_device_properties
  awscurl --service execute-api -X PUT -H "Accept: application/json" -H "Content-Type: application/json" $ENDPOINT/pools/$POOL_ID/devices/$DEVICE_ID -d @request.json
else
  cat > $TEMP_DIR/request.json << EOL
{
  "id": "$DEVICE_ID",
  "publicAddress": "$PUBLIC_ADDRESS",
  "expiresIn": "$EXPIRES_IN"
}
EOL
  __inject_device_properties
  awscurl --service execute-api -X POST -H "Accept: application/json" -H "Content-Type: application/json" $ENDPOINT/pools/$POOL_ID/devices -d @request.json
fi
rm -rf $TEMP_DIR
echo "Updated $DEVICE_ID will expires in $EXPIRES_IN"