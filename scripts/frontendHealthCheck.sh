#!/bin/bash

set -euo pipefail

find /tmp/frontend.log -mmin -20 | grep ".*" > /dev/null
