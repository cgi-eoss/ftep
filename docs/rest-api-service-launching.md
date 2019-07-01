# Launching F-TEP services via the REST API

The F-TEP service catalogue is available and can be consumed through the REST
API. Launching a service is a three step process:

1. Find the service to be launched, and its available parameters.
2. Create a Job Configuration, which instantiates a service with parameter values.
3. Launch the Job Configuration.

The launch process returns a Job, which describes the concrete execution of the
Job Configuration, including start and end times, and links to output files.

**Note**: An easy way to explore this functionality is to use your browser's
developer tools and watch the requests and responses involved in launching a
service through the [F-TEP Portal interface](https://f-tep.com/app/).

## Example

### Finding a service's parameters

Use the `https://f-tep.com/secure/api/v2.0/services` (or `.../search`) endpoint
to locate the `_links.self.href` of the service to be executed. Finding the
VegetationIndices service with `GET https://f-tep.com/secure/api/v2.0/services/search/findByFilterOnly?filter=VegetationIndices`
leads us to `https://f-tep.com/secure/api/v2.0/services/42`.

The full service definition, including parameters, can be found on the `detailedFtepService`
projection: `https://f-tep.com/secure/api/v2.0/services/42?projection=detailedFtepService`.
The `serviceDescriptor` attribute is a mapping of the WPS service attributes,
so examining the `serviceDescriptor.dataInputs` tells us the parameters, their
types, and other important information such as allowed values and
maxOccurs/minOccurs.

### Creating a Job Configuration

Armed with the service descriptor, we can build a Job Configuration object.
This is created by `POST https://f-tep.com/secure/api/v2.0/jobConfigs` with a
JSON body linking to the service and setting `inputs` parameters according to
the service descriptor.

The expected structure of the `inputs` map is an object for which each key is
the parameter id, and the value is a list of strings (even for parameters with
`maxOccurs` 1, or a non-string type).

Three optional attributes to the Job Configuration can be used to control extra
platform functionality for the input parameters. These can be explicitly set to
null/empty lists, or omitted from the request body.

* `parallelParameters`: a list of parameters to cause the Job Configuration to
  run in 'batch' mode; launching one discrete Job per value of the parameter.
* `searchParameters`: a list of parameters to be evaluated as "search form"
  objects; evaluating server-side to locate data products via an F-TEP search.
  See the [templated-service-parameters.md](templated-service-parameters.md)
  documentation for further details.
* `systematicParameter` - currently unused.

An example body looks like:

```json
{
  "service": "https://f-tep.com/secure/api/v2.0/services/42",
  "inputs": {
    "inputfile": [
      "sentinel2:///S2A_MSIL1C_20190428T095031_N0207_R079_T35VLF_20190428T115403"
    ],
    "vegIndex": [
      "NDVI"
    ],
    "aoi": [
      "POLYGON((24.153442382812496 59.11676806301213,24.153442382812496 58.8961335920761,24.592895507812496 58.8961335920761,24.592895507812496 59.11676806301213,24.153442382812496 59.11676806301213))"
    ],
    "targetResolution": [
      "10"
    ]
  },
  "systematicParameter": null,
  "parallelParameters": [],
  "searchParameters": []
}
```

The response to this POST should be a `201 Created`, and will canonically
include a `Location` header which can be visited to get the new Job
Configuration JSON. The response body will also include this JSON for easier
chaining.

### Launching a Job Configuration

To launch the job configuration, simply `POST <jobconfig self url>/launch` with
an empty request body (Note: this will be exposed as `_links.launch.href` in a
future F-TEP release).

The response to this POST should be `202 Accepted`, and the body will be the
created Job JSON.

### Awaiting Job completion

The only mechanism to detect Job completion is to periodically query the Job's
self URL to check the `status` attribute.

When complete, the same Job JSON describes the output products. They can be
seen in shorthand in the `outputs` attribute (as F-TEP-compatible URLs,
suitable for use as service input values), and more completely as
`_embedded.outputFiles` or by visiting `GET <job self url>/outputFiles`.

The output files contain useful links such as `_links.ftep.href` which may be
used as a service input value, or `_links.download.href` which offers a direct
download of the file.
