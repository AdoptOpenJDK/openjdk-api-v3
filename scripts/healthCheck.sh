#!/bin/bash

set -euo pipefail

find /tmp/updater.log -mmin -20 | grep ".*" > /dev/null
