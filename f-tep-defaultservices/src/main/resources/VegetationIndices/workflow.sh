#!/usr/bin/env bash

set -x -e

# F-TEP service environment
WORKFLOW=$(dirname $(readlink -f "$0"))
WORKER_DIR="/home/worker"
IN_DIR="${WORKER_DIR}/workDir/inDir"
OUT_DIR="${WORKER_DIR}/workDir/outDir"
WPS_PROPS="${WORKER_DIR}/workDir/FTEP-WPS-INPUT.properties"
PROC_DIR="${WORKER_DIR}/procDir"
TIMESTAMP=$(date --utc +%Y%m%d_%H%M%SZ)
EPSG2UTM="python ${WORKFLOW}/epsg2utm.py"
POLYGON2NSEW="python ${WORKFLOW}/polygon2nsewBounds.py"
S2PRODUCTZONES="python ${WORKFLOW}/s2ProductZones.py"

mkdir -p ${PROC_DIR}
mkdir -p ${OUT_DIR}/result
#mkdir -p "${OUT_DIR}/result_${VEG_INDEX}"
mkdir -p "${PROC_DIR}/preprocessed"

# Input params
source ${WPS_PROPS}
AOI="${aoi}"
INPUTFILE="${inputfile}"
TARGET_RESOLUTION="${targetResolution:-10}"
VEG_INDEX="${vegIndex}"

# extract the input product identifier (via bash parameter expansion):
INPUT_ID=${INPUTFILE##*/}

# Calculated input params
SCALING_FACTOR=$(echo "scale=2;${TARGET_RESOLUTION}/10" | bc)

# Internal params
S2_PREPROCESS="${WORKFLOW}/F-TEP_S2_preprocessNew.xml"
PREPROCESSED_PREFIX="${PROC_DIR}/preprocessed"
MOSAIC_OUTPUT="${PREPROCESSED_PREFIX}/mosaic.tif"
VI_INPUT="${PREPROCESSED_PREFIX}/vi_input.tif"
# output file name (since 25 Mar 2019; old version saved as a comment):
VI_OUTPUT="${OUT_DIR}/result/FTEP_VEGETATION_INDICES_${VEG_INDEX}_${TIMESTAMP}.tif"
#VI_OUTPUT="${OUT_DIR}/result_${VEG_INDEX}/F-TEP_${VEG_INDEX}_${TIMESTAMP}_from_${INPUT_ID}.tif"

# Bounds of given AOI
gdal_aoi_arg() {
    if [ "" != "${AOI}" ]; then
        AOI_EXTENTS=($(${POLYGON2NSEW} "${AOI}"))
        NORTH_BOUND=${AOI_EXTENTS[0]}
        SOUTH_BOUND=${AOI_EXTENTS[1]}
        EAST_BOUND=${AOI_EXTENTS[2]}
        WEST_BOUND=${AOI_EXTENTS[3]}
        UL=($(echo "${WEST_BOUND} ${NORTH_BOUND}" | cs2cs +init=epsg:4326 +to +init=epsg:${1#EPSG:}))
        LR=($(echo "${EAST_BOUND} ${SOUTH_BOUND}" | cs2cs +init=epsg:4326 +to +init=epsg:${1#EPSG:}))
        EMIN=${UL[0]}
        NMAX=${UL[1]}
        EMAX=${LR[0]}
        NMIN=${LR[1]}
        GDAL_AOI_ARG="-te ${EMIN} ${NMIN} ${EMAX} ${NMAX}"
    else
        GDAL_AOI_ARG=""
    fi
}

# Preprocess S2 input: extract correct bands and resample
I=0
for IN in ${IN_DIR}/inputfile/*; do
    I=$((I+1))
    GRANULE_METADATA=$(ls -1 ${IN}/GRANULE/*/MTD_TL.xml | head -1)
    EPSG=$(${S2PRODUCTZONES} ${GRANULE_METADATA})
    INPUT_FILE=$(ls -1 ${IN}/*.xml | grep -v 'INSPIRE.xml' | head -1)
    time gpt ${S2_PREPROCESS} -Pifile=${INPUT_FILE} -Paoi="${AOI}" -PtargetResolution="${TARGET_RESOLUTION}" -Pofile="${PREPROCESSED_PREFIX}-${I}.tif"
    time gdalwarp -t_srs ${EPSG} $(gdal_aoi_arg ${EPSG}) -tr $TARGET_RESOLUTION $TARGET_RESOLUTION -tap -ot Int16 -dstnodata "0 0 0 0" -r near -of GTiff -overwrite ${PREPROCESSED_PREFIX}-${I}.tif ${PREPROCESSED_PREFIX}-${I}r.tif
done

if [ 1 == ${I} ]; then
    mv ${PREPROCESSED_PREFIX}-${I}r.tif ${VI_INPUT}

    # Execute otb to generate radiometric index
    time otbcli_RadiometricIndices -in ${VI_INPUT} -channels.blue 1 -channels.green 2 -channels.red 3 -channels.nir 4 -list "Vegetation:${VEG_INDEX}" -out ${VI_OUTPUT}
    
    #TEST: assign nodata value of -999 instead of 0, so that Geoserver WMS can show transparency with ColorMap styling
    #gdal_edit -a_nodata -999 ${VI_OUTPUT}
    #NOTE:gdal_edit not available in gdal-bin - TODO: try translate
else
	#here, could perform e.g. mosaicking
    #echo "Single tile products only, now: ", $I
    echo "NOTE: To process multiple inputs, enable Parallel processing in service execution!"
fi

exit 0
