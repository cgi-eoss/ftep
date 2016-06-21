#!/bin/bash


INDIR="/home/worker/workdir/indir"
OUTDIR="/home/worker/workdir/outdir"
PROCDIR="/home/worker/procdir"

WORKFLOW_FILE="s2_ndvi_tif_workflow.xml"
OUTPUT_FILE_ROOT="S2_NDVI_USING_FTEP"

function checkDir {
if [ -z "$1" ]; then echo "ERROR: $2 is NOT set"; else echo "'$2' directory  is set to '$1'"; fi
}

checkDir "$INDIR"  "Input"
checkDir "$OUTDIR" "Output"
checkDir "$PROCDIR" "Processing"


#echo "The input directory is: $INDIR"
#echo "Parent Output directory is: $OUTDIR"

cd $INDIR
echo -e "List of images to be processed"
ls -1 $INDIR  |  while read p
do
inputXML=$INDIR/$p/`echo ${p/.SAFE/.xml}`
timestamp=$(date --utc +%Y%m%d_%H%M%SZ)
echo -e "gpt $PROCDIR/$WORKFLOW_FILE -Pinput1=$inputXML -Ptarget1=$OUTDIR/"${OUTPUT_FILE_ROOT}_${timestamp}.tif""
gpt $PROCDIR/$WORKFLOW_FILE -Pinput1=$inputXML -Ptarget1=$OUTDIR/"${OUTPUT_FILE_ROOT}_${timestamp}.tif"

done
