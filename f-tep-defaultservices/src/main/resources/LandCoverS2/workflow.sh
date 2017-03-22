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

# Input params
source ${WPS_PROPS}
EPSG="${crs}"
AOI="${aoi}"
DEM="${dem}"
TRAINING_SHAPEFILE=$(find -L ${IN_DIR} -name *.shp | head -1)
SHAPEFILE_ATTR="${shapefileAttribute}"
TARGET_RESOLUTION="${targetResolution}"

# Calculated input params
UTM_ZONE=$(${EPSG2UTM} ${EPSG#EPSG:})
FORMAT_NAME="SENTINEL-2-MSI-MultiRes-UTM${UTM_ZONE}"
SCALING_FACTOR=$(echo "scale=2;${TARGET_RESOLUTION}/10" | bc)

# Internal params
S2_PREPROCESS="${WORKFLOW}/S2_preprocess.xml"
S2_MOSAIC="${WORKFLOW}/S2_mosaic.xml"
PREPROCESSED_PREFIX="${PROC_DIR}/preprocessed"
MOSAIC_OUTPUT="${PROC_DIR}/mosaic.tif"
TRAINING_INPUT="${PROC_DIR}/training_input.tif"
TRAINING_OUTPUT_CLASSIFICATION_MODEL="${OUT_DIR}/FTEP_LANDCOVERS2_${TIMESTAMP}_training_model.txt"
TRAINING_OUTPUT_CONFUSION_MATRIX_CSV="${OUT_DIR}/FTEP_LANDCOVERS2_${TIMESTAMP}_confusion_matrix.csv"
OUTPUT_FILE="${OUT_DIR}/FTEP_LANDCOVERS2_${TIMESTAMP}.tif"

# Bounds of given AOI
if [ "" != "${AOI}" ]; then
    AOI_EXTENTS=($(${POLYGON2NSEW} "${AOI}"))
    NORTH_BOUND=${AOI_EXTENTS[0]}
    SOUTH_BOUND=${AOI_EXTENTS[1]}
    EAST_BOUND=${AOI_EXTENTS[2]}
    WEST_BOUND=${AOI_EXTENTS[3]}
fi

# Convert an S2 input product name to its XML metadata filename
safe2xml() {
    INPUT=$1
    IN_PROD=${INPUT/_MSIL1C_/_SAFL1C_}
    IN_PROD=${IN_PROD/_PRD_/_MTD_}
    IN_PROD=${IN_PROD/.SAFE/.xml}
    echo ${IN_PROD}
}

# Preprocess S2 input(s): extract correct bands and resample
I=0
# TODO (Eventually) figure out how to mosaic from multiple inputs
I=0
for IN in $(find -L ${IN_DIR} -type d -name 'S2*.SAFE' | head -1); do
    I=$((I+1))
    XML=$(safe2xml ${IN#${IN_DIR}/})
    INPUT_FILE="${IN}/${XML}"
    # Read the product with each possible formatName (UTM zone)
    COVERED_EPSGS=($(${S2PRODUCTZONES} ${IN}/GRANULE/*/S2*.xml))
    for PRODUCT_EPSG in $COVERED_EPSGS; do
        UTM_ZONE=$(${EPSG2UTM} ${PRODUCT_EPSG#EPSG:})
        FORMAT_NAME="SENTINEL-2-MSI-MultiRes-UTM${UTM_ZONE}"
        time gpt -c 6144M ${S2_PREPROCESS} -Pifile=${INPUT_FILE} -PformatName=${FORMAT_NAME} -Paoi="${AOI}" -PtargetResolution="${TARGET_RESOLUTION}" -Pofile="${PREPROCESSED_PREFIX}-${I}-${UTM_ZONE}.tif"
    done
done

# Preprocess S2 input(s): mosaic multiple CRS values
AOI_BOUNDS_PARAMETERS="-PnorthBound=${NORTH_BOUND} -PsouthBound=${SOUTH_BOUND} -PeastBound=${EAST_BOUND} -PwestBound=${WEST_BOUND}"
time gpt -c 6144M ${S2_MOSAIC} -t ${MOSAIC_OUTPUT} -f GeoTIFF-BigTIFF -Pepsg="${EPSG}" -Pdem="${DEM}" -PtargetResolution="${TARGET_RESOLUTION}" ${AOI_BOUNDS_PARAMETERS} ${PREPROCESSED_PREFIX}-*.tif
time gpt -c 6144M BandSelect -t ${TRAINING_INPUT} -f GeoTIFF-BigTIFF -PsourceBands=B2,B3,B4,B8 ${MOSAIC_OUTPUT}

# OTB training with "random forest" model + reference data
time otbcli_TrainImagesClassifier \
 -io.il ${TRAINING_INPUT} -io.vd ${TRAINING_SHAPEFILE} \
 -sample.mv -1 -sample.mt -1 -sample.vtr 0.5 -sample.edg false -sample.vfn ${SHAPEFILE_ATTR} \
 -classifier rf -classifier.rf.max 5 -classifier.rf.min 10 -classifier.rf.var 0 -classifier.rf.nbtrees 100 \
 -io.out ${TRAINING_OUTPUT_CLASSIFICATION_MODEL} -io.confmatout ${TRAINING_OUTPUT_CONFUSION_MATRIX_CSV}

# Final calculation using trained model
time otbcli_ImageClassifier \
 -in ${TRAINING_INPUT} \
 -model ${TRAINING_OUTPUT_CLASSIFICATION_MODEL} \
 -out ${OUTPUT_FILE}
