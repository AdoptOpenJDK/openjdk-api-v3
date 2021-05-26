#!/bin/bash

set -euo pipefail

find /logs/updater.log -mmin -20 | grep ".*" > /dev/null
