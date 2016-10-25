#!/bin/bash

INDIR="/home/worker/workDir/inDir"
OUTDIR="/home/worker/workDir/outDir"
PROCDIR="/home/worker/procDir"
WPS_FTEP_PROP_FILE="/home/worker/workDir/FTEP-WPS-INPUT.properties"

source $WPS_FTEP_PROP_FILE
echo -e $sourceBand1
echo -e $sourceBand2

WORKFLOW_FILE="s2_ndvi_tif_workflow.xml"
OUTPUT_FILE_ROOT="S2_NDVI_USING_FTEP"

function checkDir {
if [ -z "$1" ]; then echo "var is unset"; else echo "'$2' Directory  is set to '$1'"; fi
}

checkDir "$INDIR"  "Input"
checkDir "$OUTDIR" "Output"
checkDir "$PROCDIR" "Processing"

cd $INDIR
echo -e "List of images to be processed"
ls -1 $INDIR  |  while read p
do
inputXML=$INDIR/$p/`echo ${p/.SAFE/.xml}`
timestamp=$(date --utc +%Y%m%d_%H%M%SZ)
echo -e "gpt $PROCDIR/$WORKFLOW_FILE -Pinput1=$inputXML -PsourceBand1=$sourceBand1 -PsourceBand2=$sourceBand2 -Ptarget1=$OUTDIR/"${OUTPUT_FILE_ROOT}_${sourceBand1}_${sourceBand2}_${timestamp}.tif""
gpt $PROCDIR/$WORKFLOW_FILE -Pinput1=$inputXML -PsourceBand1=$sourceBand1 -PsourceBand2=$sourceBand2 -Ptarget1=$OUTDIR/"${OUTPUT_FILE_ROOT}_${sourceBand1}_${sourceBand2}_${timestamp}.tif"

done
