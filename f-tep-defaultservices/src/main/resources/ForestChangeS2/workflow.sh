#!/usr/bin/env bash

set -x -e

echo "20180508T1530"

# F-TEP service environment
WORKFLOW=$(dirname $(readlink -f "$0"))
WORKER_DIR="/home/worker"
IN_DIR="${WORKER_DIR}/workDir/inDir"
OUT_DIR="${WORKER_DIR}/workDir/outDir"
WPS_PROPS="${WORKER_DIR}/workDir/FTEP-WPS-INPUT.properties"
PROC_DIR="${WORKER_DIR}/workDir/outDir/tmp"
TIMESTAMP=$(date --utc +%Y%m%d_%H%M%SZ)

# Scripts
EXTRACT_CLOUD_MASK="python ${WORKFLOW}/extractCloudMask.py"
POLYGON2NSEW="python ${WORKFLOW}/polygon2nsewBounds.py"
EXTRACT_SRS="python ${WORKFLOW}/extractSRSfromJP2.py"
S2_PREPROCESS="${WORKFLOW}/S2_preprocess.xml"
S2_CHANGE_MAG="${WORKFLOW}/S2_changeMag.xml"

mkdir -p ${PROC_DIR}
mkdir -p ${OUT_DIR}/result

# Input params
source ${WPS_PROPS}
TARGET_CRS="${crs}"
AOI="${aoi}"
TARGET_RESOLUTION="${targetResolution}"

# Internal params
PREPROCESSED_PREFIX="${PROC_DIR}/preprocessed"
OUTPUT_FILE="${OUT_DIR}/result/FTEP_FORESTCHANGES2_${TIMESTAMP}.tif"

#STARTDIR=$(dirname ${IN_DIR}/startproduct/*)
#ENDDIR=$(dirname ${IN_DIR}/endproduct/*)
#ls -Rl ${STARTDIR}
#ls -Rl ${ENDDIR}
#START_PRODUCT=$(ls -1 ${STARTDIR}/*/*.xml | grep -v 'INSPIRE.xml' | head -1)
#END_PRODUCT=$(ls -1 ${ENDDIR}/*/*.xml | grep -v 'INSPIRE.xml' | head -1)

START_PRODUCT=$(ls -1 ${IN_DIR}/startproduct/*/*.xml | grep -v 'INSPIRE.xml' | head -1)
END_PRODUCT=$(ls -1 ${IN_DIR}/endproduct/*/*.xml | grep -v 'INSPIRE.xml' | head -1)
	## THE FOLLOWING ARE CHECKS FOR A PRODUCT PACKAGING PROBLEM DISCOVERED ON 2 MAY 2018
	if [ "" == "${START_PRODUCT}" ]; then
		START_PRODUCT=$(ls -1 ${IN_DIR}/startproduct/*/*/*.xml | grep -v 'INSPIRE.xml' | head -1)
	fi
	if [ "" == "${END_PRODUCT}" ]; then
		END_PRODUCT=$(ls -1 ${IN_DIR}/endproduct/*/*/*.xml | grep -v 'INSPIRE.xml' | head -1)
	fi

# Calculated params
START_JP2=$(dirname ${START_PRODUCT})/GRANULE/*/IMG_DATA/*_B02.jp2
END_JP2=$(dirname ${END_PRODUCT})/GRANULE/*/IMG_DATA/*_B02.jp2
START_CRS="EPSG:"$(${EXTRACT_SRS} ${START_JP2})
END_CRS="EPSG:"$(${EXTRACT_SRS} ${END_JP2})

START_CLOUD_GML=$(dirname ${START_PRODUCT})/GRANULE/*/QI_DATA/MSK_CLOUDS_B00.gml
END_CLOUD_GML=$(dirname ${END_PRODUCT})/GRANULE/*/QI_DATA/MSK_CLOUDS_B00.gml

START_CLOUD_MASK="${PROC_DIR}/start_cloud_mask.tif"
END_CLOUD_MASK="${PROC_DIR}/end_cloud_mask.tif"

# Check that the input products have the same CRS
if [ "${START_CRS}" != "${END_CRS}" ]; then
	echo "Start and end products have different coordinate reference systems (CRS):" ${START_CRS} "vs." ${END_CRS}
    echo "Please select input files with identical CRS (the same UTM zone)."
    exit 0
fi

# START image: create cloud mask - an empty mask if there is no cloud info in the product metadata
if grep -q MaskFeature ${START_CLOUD_GML}; then
	echo "START image: Cloud info found. Creating cloud mask."
    time ${EXTRACT_CLOUD_MASK} $(dirname ${START_PRODUCT})/GRANULE/*/IMG_DATA/*_B02.jp2 ${START_CLOUD_GML} ${START_CLOUD_MASK}
else
	echo "START image: Cloud info NOT found. Creating an empty mask."
	time gdal_translate -of GTiff -ot Byte -scale 0 32768 0 0 $(dirname ${START_PRODUCT})/GRANULE/*/IMG_DATA/*_B02.jp2 ${START_CLOUD_MASK}
fi

# END image: create cloud mask - an empty mask if there is no cloud info in the product metadata
if grep -q MaskFeature ${END_CLOUD_GML}; then
	echo "END image: Cloud info found. Creating cloud mask."
    time ${EXTRACT_CLOUD_MASK} $(dirname ${END_PRODUCT})/GRANULE/*/IMG_DATA/*_B02.jp2 ${END_CLOUD_GML} ${END_CLOUD_MASK}
else
	echo "END image: Cloud info NOT found. Creating an empty mask."
	time gdal_translate -of GTiff -ot Byte -scale 0 32768 0 0 $(dirname ${END_PRODUCT})/GRANULE/*/IMG_DATA/*_B02.jp2 ${END_CLOUD_MASK}
fi

# Apply cloud masks
echo "START image: Applying the mask."
time gpt ${S2_PREPROCESS} -Pifile=${START_PRODUCT} -Pmsk=${START_CLOUD_MASK} -Pofile="${PREPROCESSED_PREFIX}-start.tif"
echo "END image: Applying the mask."
time gpt ${S2_PREPROCESS} -Pifile=${END_PRODUCT} -Pmsk=${END_CLOUD_MASK} -Pofile="${PREPROCESSED_PREFIX}-end.tif"

# Compute change detection
echo "Performing the change detection."
time gpt ${S2_CHANGE_MAG} -Pstart="${PREPROCESSED_PREFIX}-start.tif" -Pend="${PREPROCESSED_PREFIX}-end.tif" -Pofile=${OUTPUT_FILE}

# DEBUG output package
#zip ${OUT_DIR}/result/forestChange_DEBUG_${TIMESTAMP}.zip ${PROC_DIR}/* ${START_CLOUD_GML} ${END_CLOUD_GML} ${OUTPUT_FILE}; rm ${OUTPUT_FILE}

##########
# Adjusting with CRS(EPSG), AOI, resolution

OUTPUT_FINAL_FILE="${OUT_DIR}/result/FTEP_FORESTCHANGES2_FINAL_${TIMESTAMP}.tif"

#### If all optional parameters are empty, do not do anything
if [ "" == "${TARGET_CRS}" ] && [ "" == "${AOI}" ] && [ "" == "${TARGET_RESOLUTION}" ]; then
	mv ${OUTPUT_FILE} ${OUTPUT_FINAL_FILE}
	ls -al ${OUT_DIR}/result/
	exit 0
fi

echo "Adjusting with CRS(EPSG), AOI, resolution."

# CRS(EPSG)
if [ "" == "${TARGET_CRS}" ]; then
    TARGET_CRS=${END_CRS}
fi
WARP_SRS="-t_srs ${TARGET_CRS}"

# AOI
if [ "" != "${AOI}" ]; then
    AOI_EXTENTS=($(${POLYGON2NSEW} "${AOI}"))
    NORTH_BOUND=${AOI_EXTENTS[0]}
    SOUTH_BOUND=${AOI_EXTENTS[1]}
    EAST_BOUND=${AOI_EXTENTS[2]}
    WEST_BOUND=${AOI_EXTENTS[3]}
	UL=($(echo "${WEST_BOUND} ${NORTH_BOUND}" | cs2cs +init=epsg:4326 +to +init=epsg:${TARGET_CRS#EPSG:}))
	LR=($(echo "${EAST_BOUND} ${SOUTH_BOUND}" | cs2cs +init=epsg:4326 +to +init=epsg:${TARGET_CRS#EPSG:}))
	EMIN=${UL[0]}
	NMAX=${UL[1]}
	EMAX=${LR[0]}
	NMIN=${LR[1]}
	WARP_EXTENT="-te ${EMIN} ${NMIN} ${EMAX} ${NMAX}"
fi

# Resolution
if [ "" != "${TARGET_RESOLUTION}" ]; then
	WARP_RESOLUTION="-tr ${TARGET_RESOLUTION} ${TARGET_RESOLUTION} -tap"
fi

# Finally:
echo "Adjusting with CRS(EPSG), AOI, resolution."
time gdalwarp ${WARP_SRS} ${WARP_EXTENT} ${WARP_RESOLUTION} -ot Int16 -dstnodata "0 0 0 0" -r near -of GTiff -overwrite ${OUTPUT_FILE} ${OUTPUT_FINAL_FILE}
ls -al ${OUT_DIR}/result/
rm ${OUTPUT_FILE}

exit 0
