#! /bin/bash
#
# Copyright (c) 2022 Philip Cali
# Released under Apache-2.0 License
#     (https://www.apache.org/licenses/LICENSE-2.0)
#

EXEC_NAME="device-lab"
ARTIFACTS_DIR="$HOME/bin/artifacts"
DEVICE_LAB_BIN="$HOME/bin/$EXEC_NAME"
MAVEN_REPO="https://artifacts.philcali.me/maven"
# Fix for release
ARTIFACT_VERSION="1.0.0"
ARTIFACTS_URL="$MAVEN_REPO/release/me/philcali/device-pool-cli/$ARTIFACT_VERSION/device-pool-cli-$ARTIFACT_VERSION.jar"

if [ ! -f $ARTIFACTS_DIR ]; then
  echo "Creating $ARTIFACTS_DIR for device-lab"
  mkdir -p $ARTIFACTS_DIR
fi

echo "Installing necessary artifact"
curl $ARTIFACTS_URL > "$ARTIFACTS_DIR/device-lab.jar"

echo "Installing $EXEC_NAME in $DEVICE_LAB_BIN"
curl https://raw.githubusercontent.com/philcali/device-pool/main/device-pool-cli/scripts/device-lab > $DEVICE_LAB_BIN
chmod +x $DEVICE_LAB_BIN

echo "Initiate by running $HOME/bin/$EXEC_NAME"
echo "device-lab help"
echo "Done"
echo "#########################"

$HOME/bin/device-lab help