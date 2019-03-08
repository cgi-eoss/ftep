#!/usr/bin/env bash
# argument 1 (if present) is "worker dir"

set -x -e

# F-TEP service environment

WORKFLOW=$(dirname $(readlink -f "$0"))
if [[ $# -gt 0 ]]; then
    WORKER_DIR="$1"
else
    WORKER_DIR="/home/worker"
fi
WORKFLOW=$(dirname $(readlink -f "$0"))
IN_DIR="${WORKER_DIR}/workDir/inDir"
OUT_DIR="${WORKER_DIR}/workDir/outDir"
WPS_PROPS="${WORKER_DIR}/workDir/FTEP-WPS-INPUT.properties"
PROC_DIR="${WORKER_DIR}/procDir"
TIMESTAMP=$(date --utc +%Y%m%d_%H%M%SZ)
EPSG2UTM="python3 ${WORKFLOW}/epsg2utm.py"
POLYGON2NSEW="python3 ${WORKFLOW}/polygon2nsewBounds.py"
UNIQUEORBITS="python3 ${WORKFLOW}/uniqueorbits.py"
YRS1ORBIT="python3 ${WORKFLOW}/yrs1orbit.py"
YRTAP="python3 ${WORKFLOW}/yrtap.py"

mkdir -p ${PROC_DIR}
mkdir -p ${OUT_DIR}/result

# Input parameters

if [[ $# -eq 0 ]]; then
    source ${WPS_PROPS}
fi
AOI="${aoi}"
if [[ "" != ${ext_dem} ]]; then
    DEM=$(ls -1 ${IN_DIR}/ext_dem/*.tif | head -1)
    DIR_TEMP2="$(readlink -f ${IN_DIR}/ext_dem)"
    DIR_TEMP3="$(ls -1 ${DIR_TEMP2})"
    DIR_TEMP4="$(readlink -f ${DIR_TEMP2}/${DIR_TEMP3})"
    TRAINING_LEAVE="$(ls -1 ${DIR_TEMP4})"
    DEM="${DIR_TEMP4}/${TRAINING_LEAVE}"
    S1_PREPROCESS="${WORKFLOW}/S1mosPreprocessExt.xml"
else
    DEM="SRTM 1Sec HGT"
    S1_PREPROCESS="${WORKFLOW}/S1mosPreprocessSRTM.xml"
fi

TARGET_RESOLUTION="${targetResolution:-10}"
EPSG="${crs}"

# Internal parameters

PREPROCESSED_PREFIX="${PROC_DIR}/preprocessed"
OUTPUT_FILE="${OUT_DIR}/result/FTEP_${outname}_${TIMESTAMP}.tif"

# Bounds of given AOI

if [[ "" != "${AOI}" ]]; then
    AOI_EXTENTS=($(${POLYGON2NSEW} "${AOI}"))
    NORTH_BOUND=${AOI_EXTENTS[0]}
    SOUTH_BOUND=${AOI_EXTENTS[1]}
    EAST_BOUND=${AOI_EXTENTS[2]}
    WEST_BOUND=${AOI_EXTENTS[3]}
fi
UL=($(echo "${WEST_BOUND} ${NORTH_BOUND}" | cs2cs +init=epsg:4326 +to +init=epsg:${EPSG#EPSG:}))
LR=($(echo "${EAST_BOUND} ${SOUTH_BOUND}" | cs2cs +init=epsg:4326 +to +init=epsg:${EPSG#EPSG:}))
EMIN=${UL[0]}
NMAX=${UL[1]}
EMAX=${LR[0]}
NMIN=${LR[1]}
YT=($(${YRTAP} ${TARGET_RESOLUTION} ${EMIN} ${EMAX} ${NMIN} ${NMAX}))
EMIN=${YT[0]}
EMAX=${YT[1]}
NMIN=${YT[2]}
NMAX=${YT[3]}
ls -o ${IN_DIR}/inputfiles

# Preprocess S1 input(s)

IN_DIR2="$(readlink -f ${IN_DIR}/inputfiles)"
a=()
a=($(find -L "$IN_DIR2" -name manifest.safe))
n=${#a[@]}
for (( k=0; k<n ; k++ ))
    do
    ls -o "${a[(($k))]}"
    at=($(${YRS1ORBIT} "${a[(($k))]}"))
    s1orbit[(($k))]=${at[0]}
    done
ct="${s1orbit[@]}"
ctt="${ct// /,}"
d=($(${UNIQUEORBITS} "${ctt}"))
m=${#d[@]}
echo ${n} "images," ${m} "orbits:" "${d[@]}"
II=0
for (( i=0; i < n; i++ ))
do echo $i "${a[$i]}" "${s1orbit[$i]}"
done

rm -f ${PROC_DIR}/vrtlist2.txt
for i in ${d[@]}
    do 
    echo "Processing orbit" ${i}
    rm -f ${PROC_DIR}/vrtlist.txt
    II=$((II+1))
    JJ=0
	for (( k=0; k<n ; k++ ))
        do
	    if [[ ${s1orbit[((${k}))]} == ${i} ]]; then
		echo "Processing image: ${a[(($k))]}"
	    	INPUT_FILE="${a[(($k))]}"
	    	JJ=$((JJ+1))
	    	time gpt -c 4G -q 3 ${S1_PREPROCESS} -Pifile="${INPUT_FILE}" \
			-PdemFile="${DEM}" -PtargetResolution="${TARGET_RESOLUTION}" \
			-Paoi="${AOI}" -Pofile="${PREPROCESSED_PREFIX}-${JJ}.tif"
	    	time gdal_translate -scale 0 1 0 10000 -of GTiff -ot UInt16 \
			"${PREPROCESSED_PREFIX}-${JJ}.tif" \
			"${PREPROCESSED_PREFIX}-${JJ}s.tif"
	    	rm "${PREPROCESSED_PREFIX}-${JJ}.tif"
	    	time gdalwarp -t_srs ${EPSG} -te ${EMIN} ${NMIN} \
			${EMAX} ${NMAX} -tr ${TARGET_RESOLUTION} \
			${TARGET_RESOLUTION} -tap -ot UInt16 \
			-dstnodata "0 0" -srcnodata "0 0" -r bilinear -of GTiff \
			-co BIGTIFF=YES -overwrite ${PREPROCESSED_PREFIX}-${JJ}s.tif \
			${PREPROCESSED_PREFIX}-${JJ}r.tif
	    	rm "${PREPROCESSED_PREFIX}-${JJ}s.tif"
	    	echo ${PREPROCESSED_PREFIX}-${JJ}r.tif >> ${PROC_DIR}/vrtlist.txt
	    fi
	done

#   Combine scenes of an obit with gdal

    cat ${PROC_DIR}/vrtlist.txt
    time gdalbuildvrt -input_file_list ${PROC_DIR}/vrtlist.txt \
	-vrtnodata "0 0" -overwrite ${PROC_DIR}/tmp.vrt
    time gdalwarp -t_srs ${EPSG} -te $EMIN $NMIN $EMAX $NMAX \
	-tr $TARGET_RESOLUTION $TARGET_RESOLUTION -tap -ot UInt16 \
	-dstnodata "0 0" -srcnodata "0 0" -r near -of GTiff -co \
	BIGTIFF=YES -overwrite ${PROC_DIR}/tmp.vrt \
	${PREPROCESSED_PREFIX}-epoch_${II}.tif
    time gdal_translate -b 1 ${PREPROCESSED_PREFIX}-epoch_${II}.tif \
	${PREPROCESSED_PREFIX}-epoch_${II}b1.tif
    time gdal_translate -b 2 ${PREPROCESSED_PREFIX}-epoch_${II}.tif \
	${PREPROCESSED_PREFIX}-epoch_${II}b2.tif
    echo ${PREPROCESSED_PREFIX}-epoch_${II}b1.tif \
	>> ${PROC_DIR}/vrtlist2.txt    
    echo ${PREPROCESSED_PREFIX}-epoch_${II}b2.tif \
	>> ${PROC_DIR}/vrtlist2.txt    
    rm "${PREPROCESSED_PREFIX}-epoch_${II}.tif"
    done
rm ${PREPROCESSED_PREFIX}-*r.tif

# Combine epochs with gdal

cat ${PROC_DIR}/vrtlist2.txt
time gdalbuildvrt -input_file_list ${PROC_DIR}/vrtlist2.txt \
    -separate -vrtnodata "0 0" -overwrite ${PROC_DIR}/tmp2.vrt
time gdalwarp -t_srs ${EPSG} -te $EMIN $NMIN $EMAX $NMAX \
    -tr $TARGET_RESOLUTION $TARGET_RESOLUTION -tap -ot UInt16 \
    -dstnodata "0 0" -r near -of GTiff -co BIGTIFF=YES -overwrite \
    ${PROC_DIR}/tmp2.vrt ${OUTPUT_FILE}
ls -o "${OUTPUT_FILE}"
rm ${PREPROCESSED_PREFIX}-epoch_*b*.tif
exit 0