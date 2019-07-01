# Templated service parameters

F-TEP service developers can optionally use an advanced service configuration
to access powerful platform functionality. This takes the form of a secondary
parameterisation mode, accessible through the standard service editor.

## Principle

The "Input definitions" tab in the service editor is a close mapping of the
[WPS 2.0](WPS) service parameter model. These are referred to as "canonical"
inputs throughout this document.

While this canonical input parameterisation is generally sufficient for running
processing jobs, F-TEP offers some additional dynamic parameterisation features
which are not expressible through this schema.

The advanced features are exposed through two aspects:

* The F-TEP server may detect Job Configuration attributes to trigger dynamic
  parameter evaluation, currently: batch expansion (for parallel Jobs from a
  single configuration) and search parameter expansion (to dynamically evaluate
  searches to populate input data fields).
* Service developers may define a second set of possible input parameters, plus
  a template to translate user-given values for these into the canonical
  parameter schema in a Job Configuration object. This allows service
  developers to exploit the advanced Job Configuration attributes.

The advanced parameter configuration attributes are made possible by
configuring the canonical service inputs to enable them; in this way specific
service attributes can be permitted to be used for batch processing, or as
search-driven input parameters.

The secondary input interface is described as "Simple input definitions", as it
is expected that most service developers will use the templating feature to
offer an "easier" configuration mode to users. However this is not necessarily
the case: there may be more parameters, or more advanced parameters, which are
used to generate the canonical input parameters for the service.

See below for a comprehensive example exploiting both dynamic parameter
features.

### Batch processing

A canonical service input can be permitted to launch batch/parallel processing;
a multiplicity operator.

UI: When enabled, the user will see a checkbox when creating the Job
Configuration. If checked, the Job will execute in 'parallel' mode. Each value
of the parameter will result in its own Sub-Job, in which the other parameters
are kept constant. The results may be viewed individually or as a group from
the parent Job. These Sub-Jobs execute concurrently according to the platform
capacity.

API: This corresponds to the `parallelParameters` JSON attribute in the Job
Configuration object. This list-valued attribute should contain the canonical
service inputs which are to be expanded into multiple Sub-Jobs.

### Search-driven parameters

A canonical service input can execute a standard F-TEP search during the launch
process. The results of the search are used as values for the parameter.

Each value of the input parameter is expected as a URL-encoded key-value pair,
exactly as those sent by the UI when searching via the F-TEP web interface.
By selecting appropriate default search parameters, the service can *auto-pick
its best data*.

API-only: The Job Configuration `searchParameters` JSON attribute controls
which attributes should be evaluated as an F-TEP search during launch.

## Template format

The template to convert "Simple input definitions" into a Job Configuration
with canonical input parameters is handled client-side before the Job
Configuration is launched. The format is [Handlebars](Handlebars), a powerful
JavaScript templating language.

The service developer interface offers a preview mode, so the service developer
can verify what end-user input values will look like when run through the
template.

## Example

A VegetationIndices service offers the following parameters as canonical input
parameters:

```json
[ {
  "id" : "inputfile",
  "title" : "Input data",
  "description" : "Sentinel-2 data file(s)",
  "minOccurs" : 1,
  "maxOccurs" : 50,
  "data" : "LITERAL",
  "defaultAttrs" : {
    "dataType" : "string"
  },
  "supportedAttrs" : null,
  "dataReference" : true,
  "parallelParameter" : true
}, {
  "id" : "vegIndex",
  "title" : "Radiometric index algorithm",
  "description" : "Vegetation index to calculate",
  "minOccurs" : 1,
  "maxOccurs" : 1,
  "data" : "LITERAL",
  "defaultAttrs" : {
    "dataType" : "string",
    "allowedValues" : "GEMI,IPVI,MSAVI,MSAVI2,NDVI,RVI,SAVI,TNDVI,TSAVI",
    "value" : "NDVI"
  },
  "supportedAttrs" : null
}, {
  "id" : "aoi",
  "title" : "Area of interest",
  "description" : "AOI to be processed, in the well-known text (WKT) format, e.g. POLYGON((...))",
  "minOccurs" : 0,
  "maxOccurs" : 1,
  "data" : "LITERAL",
  "defaultAttrs" : {
    "dataType" : "string"
  },
  "supportedAttrs" : null
}, {
  "id" : "targetResolution",
  "title" : "Output pixel spacing",
  "description" : "Request output spacing in metres, e.g. 10 or 20",
  "minOccurs" : 1,
  "maxOccurs" : 1,
  "data" : "LITERAL",
  "defaultAttrs" : {
    "dataType" : "string"
  },
  "supportedAttrs" : null
} ]
```

The service developer would like to expose a simple configuration mode, which
allows the end-user to avoid understanding what input data is appropriate, and
minimises other parameters. They click the "+" icon on the service editor, and
configure the following "Simple input parameters":

```json
[ {
  "id" : "aoi",
  "title" : "Area Of Interest",
  "description" : "AOI to be processed, in the Well-known text (WKT) format, e.g. POLYGON(( etc.",
  "minOccurs" : 1,
  "maxOccurs" : 1,
  "data" : "LITERAL",
  "defaultAttrs" : {
    "dataType" : "string"
  },
  "supportedAttrs" : null
}, {
  "id" : "startDate",
  "title" : "Start date",
  "description" : "Start date for satellite product search (in yyyy-MM-dd format)",
  "minOccurs" : 1,
  "maxOccurs" : 1,
  "data" : "LITERAL",
  "defaultAttrs" : {
    "dataType" : "string"
  },
  "supportedAttrs" : null
}, {
  "id" : "endDate",
  "title" : "End date",
  "description" : "End date for satellite product search (in yyyy-MM-dd format)",
  "minOccurs" : 1,
  "maxOccurs" : 1,
  "data" : "LITERAL",
  "defaultAttrs" : {
    "dataType" : "string"
  },
  "supportedAttrs" : null
} ]
```

In order to translate this into the canonical input values, the service
developer decides to use the `searchParameters` mode, with sensible defaults to
search for good data to use for their VegetationIndices service. The developer
also wishes to allow the user to analyse _all_ valid data across the specified
AOI and date range, by running the service over each search result in parallel.
However this is limited to the 10 best products.

The developer then visits the F-TEP UI and uses the browser's developer tools to
identify what an appropriate F-TEP search request should look like.

The resulting template looks like:

```json
{
  "inputs": {
    "inputfile": [
      "productDateStart={{inputs.startDate.[0]}}T00:00:00.000",
      "productDateEnd={{inputs.endDate.[0]}}T23:59:59.000",
      "maxCloudCover=10",
      "maxRecords=10",
      "aoi={{inputs.aoi.[0]}}",
      "catalogue=SATELLITE",
      "mission=sentinel2",
      "s2ProcessingLevel=1C",
      "sortParam=cloudCover",
      "sortOrder=ascending"
    ],
    "vegIndex": [
      "NDVI"
    ],
    "targetResolution": [
      "20"
    ],
    "aoi": [
      "{{inputs.aoi.[0]}}"
    ]
  },
  "searchParameters": [
    "inputfile"
  ],
  "parallelParameters": [
    "inputfile"
  ]
}
```

On launch, if the user specified the parameters:

* `aoi` = `POLYGON((23.4 61.5,23.4 60.5,25.7 60.5,25.7 61.5,23.4 61.5))`
* `startDate` = `2019-03-01`
* `endDate` = `2019-03-31`

the resulting Job Configuration would be generated as:

```json
{
  "inputs": {
    "inputfile": [
      "productDateStart=2019-03-01T00:00:00.000Z",
      "productDateEnd=2019-03-31T23:59:59.999Z",
      "maxCloudCover=10",
      "maxRecords=1",
      "aoi=POLYGON((23.4%2061.5,23.4%2060.5,25.7%2060.5,25.7%2061.5,23.4%2061.5))",
      "catalogue=SATELLITE",
      "mission=sentinel2",
      "s2ProcessingLevel=1C",
      "sortParam=cloudCover",
      "sortOrder=ascending"
    ],
    "vegIndex": [
      "NDVI"
    ],
    "targetResolution": [
      "20"
    ],
    "aoi": [
      ""
    ]
  },
  "searchParameters": [
    "inputfile"
  ],
  "service": "https://f-tep.com/secure/api/v2.0/services/42",
  "parallelParameters": [
    "inputfile"
  ],
  "systematicParameter": null
}
```

During the launch process, F-TEP will evaluate in order the `searchParameters`
and `parallelParameters` attributes.

First it will detect the `searchParameters` attribute and evaluate this search,
which returns the 10 lowest-cloudiness products in the given AOI/TOI, and
assign the 10 results as the input values for the `inputfile` canonical service
parameter.

Next, the `parallelParameters` attribute is used to creat 10 Sub-Jobs, one per
value of the `inputfile` parameter.

The resulting 10 Jobs from these transformations are executed as normal, and
the results can be viewed in the normal manner.

[WPS]: https://www.opengeospatial.org/standards/wps
[Handlebars]: https://handlebarsjs.com/
