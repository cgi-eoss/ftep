# Systematic processing

All F-TEP services can optionally be configured to run in "Systematic" mode,
allowing automated, scheduled detection of input data and generation of
processing jobs.

## Principle

Systematic processing is enabled on all PROCESSOR services, and provides the
user with the ability to supply a single service parameter value with an F-TEP
search configuration.

This overlaps slightly with the [templated-service-parameters.md](templated-service-parameters.md)
feature, but uniquely enables repeated evaluation of the search, even into the
future.

Upon launching a service with this mode, a new systematic processing
configuration is created in the platform. This results in a batch-style job,
where each search result from the configuration results in a sub-job.

The platform periodically evaluates all active systematic processing
configurations, detecting new data and generating the sub-jobs accordingly.

Systematic processing configurations may be inspected and paused or deleted on
the Manage/Share tab.

**Note:** If a service has an alternative templated configuration mode,
systematic processing may only be used when the service is being configured in
"Advanced" mode.

## Example

A user wants to generate a continuous timeline of radiometric vegetation index
products for a given Sentinel-2 tile identifier within a time window.

1. Select the "VegetationIndices" service
2. Toggle "Advanced Mode" in the service header
3. Switch the "Processor running mode" to "Systematic"
4. Select the dynamic input parameter to be populated by the search, in this
  case "Input data"
5. Configure the service as normal, leaving "Input data" and "AOI" blank (for
  full-tile output products)
6. In the systematic search configuration, select a "Product start date" and
  optionally "Product end date" for systematic monitoring, and add the required
  search parameters.
7. Click "Launch"

### Example parameters

* Radiometric index algorithm: `NDVI`
* Output pixel spacing: `20`
* Product start date: `01-06-2019`
* Product end date: `01-06-2020`
* Mission: `Sentinel-2`
* Identifier: `T34VFM` (desired S-2 tile id)
* AOI: `POINT(23.46 60.90)` (a point within the tile; optional)
* Cloud coverage: `10`
* Processing level: `Level 1`

Upon first iteration, the systematic processing configuration will spawn jobs
for all products found between "Product start date" and "now". Repeated
searches will generate new jobs for any un-processed data, up to the limit
defined by "Product end date".
