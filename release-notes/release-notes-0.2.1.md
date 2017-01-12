# Release notes for F-TEP 0.2.1

F-TEP 0.2.1 is the first hotfix release for the 0.2 branch. It resolves critical issues and improvements identified
during initial user introduction, and contains some service improvements.

## Improvements &amp; Changes

* Several improvements to VegetationIndices and LandCoverS2 services:
  * TEPF-100: VegetationIndices takes an AOI parameter
  * TEPF-101: LandCoverS2 &amp; VegetationIndices use BigTIFF output to support processing tasks covering a larger area
  * VegetationIndices requires an EPSG CRS parameter, instead of the UTM zone
  * VegetationIndices performs resampling operation in pre-processing, rather than post-processing
* TEPF-102: The obsolete Sentinel2Ndvi processor service has been removed
* TEPF-104: Sentinel-2 search provides default value for max cloudiness (5%)
* TEPF-105: Platform version is now indicated

## Bug fixes

* TEPF-30: Map layer selection correctly shows the selected base map
* TEPF-31: AOI selection is retained across map type changes
* TEPF-79: Service info pop-up no longer jumps off-window
* Use correct projection for on-map visibility of job output product previews
* Use correct projections for search polygon and its WKT string representation
