# Release notes for F-TEP 0.4.2

F-TEP 0.4.2 is the second hotfix release for the 0.4 branch and the first
production release of this branch.

## Processor services

The following processors have been introduced:

* LandCoverS1
* S1Biomass

## Improvements &amp; Changes

* TEPF-28: Link 'Helpdesk' to the user manual in the F-TEP Drupal site
* TEPF-32: Triggering a Job automatically opens the Jobs panel
* TEPF-44: Collect status messages into a footer panel
* TEPF-49: Improve display of input parameters for service configuration
* TEPF-72: Non-applicable search parameters are hidden
* TEPF-74: No longer separate searches for different satellites of one mission
* TEPF-87: Results panel more clearly distinguishes between types of products
* TEPF-88: Rename 'Products' to 'Existing Products' for clarity
* TEPF-101: GeoTIFF-BigTIFF is used for processor outputs
* TEPF-104: Add default value for max cloudiness in S-2 search
* TEPF-105: Platform version is indicated in the page header
* Sentinel-2 processors will mosaic an AOI across multiple UTM zones (within a single product)
* Minor tweaks to processing step order and parameter efficiency

## Bug fixes

* TEPF-30: Selected map layer is correctly shown
* TEPF-31: AOI selection is more reliable
* TEPF-79: Service info popup no longer jumps off-window
* TEPF-81: Search result footprint highlighting is more reliable
* Many more minor fixes for UI issues

