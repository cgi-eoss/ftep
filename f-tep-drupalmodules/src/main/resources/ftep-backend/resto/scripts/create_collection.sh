#!/usr/bin/env bash
CREDENTIALS="admin:admin"

curl -i -k -X POST -H "Content-Type: application/json" \
    -d @./collections/$1.json \
    "http://localhost/resto/collections" -u ${CREDENTIALS}
