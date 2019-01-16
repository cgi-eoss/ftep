#!/bin/sh

WORKER_DIR="/home/worker"
IN_DIR="${WORKER_DIR}/workDir/inDir"
OUT_DIR="${WORKER_DIR}/workDir/outDir"

# Workaround missing .SEN3 dir suffix
S3_PRODUCTS=""
for s3_product in $(find -L ${IN_DIR} -maxdepth 3 -path '*/S3*/xfdumanifest.xml'); do
  product_dir="$(dirname $s3_product)"
  link_dir="/tmp/$(basename $product_dir).SEN3"
  ln -s $product_dir $link_dir
  S3_PRODUCTS="${S3_PRODUCTS} $link_dir/xfdumanifest.xml"
done

INPUT_FILES="$(find -L ${IN_DIR} -maxdepth 3 -name '*MTD*.xml' -or -iname '*.tif') ${S3_PRODUCTS}"

ln -snf ${IN_DIR} /nobody/inDir
ln -snf ${OUT_DIR} /nobody/outDir

cd "${WORKER_DIR}/workDir"
/opt/snap/bin/snap --open ${INPUT_FILES}
