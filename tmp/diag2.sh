#!/usr/bin/env bash
cd "/data/data/com.meowcode.app/files/projects/html và csss" || exit 1
echo "=== shells ==="
ls -l /usr/bin/sh /usr/bin/env /system/bin/sh 2>&1
echo "=== sh while ==="
sh -c 'i=0; while [ $i -lt 2 ]; do echo W=$i; i=$((i+1)); done' 2>&1
echo "=== tools ==="
for t in curl wget unzip zip aapt2 d8 apksigner zipalign python3 node; do printf '%s=%s\n' "$t" "$(command -v $t)"; done
echo "=== build_config ==="
cat .meowcode/build_config.json 2>&1
echo "=== README head ==="
head -40 README.md 2>&1
echo "=== tools dir ==="
ls -l tools 2>&1
echo "=== ok ==="
