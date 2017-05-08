#!/bin/bash

gpg --list-keys --with-colons --with-fingerprint `gpg --list-keys \
--with-colons | grep "pub:f:.*:-:" | sed -r -e \
's/pub:f:[0-9]+:[0-9]+:([A-F0-9]+):.*/0x\1/'` | grep "^fpr:" \
| sed -r -e 's/fpr:::::::::([0-9A-F]+):/\1:6:/' | gpg --import-ownertrust