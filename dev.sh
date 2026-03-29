#!/usr/bin/env bash
set -euo pipefail

plackup -R lib -Ilib --port 5000 bin/fork.psgi
