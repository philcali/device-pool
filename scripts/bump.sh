#! /bin/bash
#
# Copyright (c) 2022 Philip Cali
# Released under Apache-2.0 License
#     (https://www.apache.org/licenses/LICENSE-2.0)
#

FROM_VERSION=$1
TO_VERSION=$2

find . -type f \
 | grep -v "target" \
 | grep -v ".git" \
 | grep -v "native-libs" \
 | grep -v "install\." \
 | xargs sed -i "s/$FROM_VERSION/$TO_VERSION/"
