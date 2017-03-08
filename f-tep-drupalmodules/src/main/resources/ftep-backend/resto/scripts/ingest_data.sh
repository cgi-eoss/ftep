#!/usr/bin/env bash
CREDENTIALS="admin:admin"

curl -i -k -X POST -d@$2 http://localhost/resto/collections/$1 -u ${CREDENTIALS}