#! /bin/bash

EXEC_NAME="device-lab"
ARTIFACTS_DIR="$HOME/bin/artifacts"
DEVICE_LAB_BIN="$HOME/bin/$EXEC_NAME"
MAVEN_REPO="https://artifacts.philcali.me/maven"
# Fix for release
ARTIFACTS_URL="$MAVEN_REPO/snapshot/me/philcali/device-pool-cli/1.0-SNAPSHOT/device-pool-cli-1.0-20220218.221758-11.jar"

if [ ! -f $ARTIFACTS_DIR ]; then
  echo "Creating $ARTIFACTS_DIR for device-lab"
  mkdir -p $ARTIFACTS_DIR
fi

echo "Installing necessary artifact"
curl $ARTIFACTS_URL > "$ARTIFACTS_DIR/device-lab.jar"

echo "Installing $EXEC_NAME in $DEVICE_LAB_BIN"
curl https://raw.githubusercontent.com/philcali/device-pool/main/device-pool-cli/scripts/device-lab > $DEVICE_LAB_BIN
chmod +x $DEVICE_LAB_BIN

echo "Initiate by running $EXEC_NAME"
echo "device-lab help"
echo "Done"