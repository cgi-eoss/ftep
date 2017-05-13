#!/bin/sh

WORKER_DIR="/home/worker"
IN_DIR="${WORKER_DIR}/workDir/inDir"
OUT_DIR="${WORKER_DIR}/workDir/outDir"

INPUT_FILES=$(find -L ${IN_DIR} -maxdepth 3 -iname '*.tif')

ln -snf ${IN_DIR} /nobody/inDir
ln -snf ${OUT_DIR} /nobody/outDir

/usr/bin/qgis ${INPUT_FILES}
