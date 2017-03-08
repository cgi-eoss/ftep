#!/usr/bin/env bash

BASE_URL=http://localhost/resto
COLLECTION_LIST="ftepref ftepinput ftepoutput"
CREDENTIALS="admin:admin"

for COLLECTION in $COLLECTION_LIST; do
	echo "Cleaning: $COLLECTION"
	for i in $( curl -s $BASE_URL/api/collections/$COLLECTION/search.json | jq  -r '.features | .[] | .id'   ); do
		curl -i -k -X DELETE  "$BASE_URL/collections/$COLLECTION/$i " -u ${CREDENTIALS}
	done

	curl -i -k -X DELETE  "$BASE_URL/collections/$COLLECTION" -u ${CREDENTIALS}

done