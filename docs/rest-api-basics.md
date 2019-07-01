# REST API Basics

The current public version of the F-TEP REST interface is based on [Spring Data
REST](https://spring.io/projects/spring-data-rest) and can be found at
https://f-tep.com/secure/api/v2.0/. Visiting the API in a web browser should
open up the HAL Browser, a basic web interface for navigating the API.

**Note**: An easy way to explore the API is to use your browser's developer
tools and watch the requests and responses when using the [F-TEP Portal
interface](https://f-tep.com/app/).

## Concepts

Communication with the REST API is expected to be in JSON format; technically,
`application/hal+json`. This provides a few well-understood elements and
attributes which can be used to traverse the API.

### Profile

The API generates a `profile` document, effectively a schema describing what
the various endpoints return. This is visible at `https://f-tep.com/secure/api/v2.0/profile/`.

### `_links`

Entities and messages in the JSON responses should contain `_links` attributes
which describe related API endpoints. These links are constructed based on the
currently-accessed URL. Any query parameters (e.g. for searching) are displayed
in the link.

**Important**: Whenever entity references are required, e.g. as an attribute
in POSTed JSON, or as query parameters, the `_links.self.href` of the entity in
question should be used. See the example below.

### `_embedded`

When entities are embedded in the response, or when a collection of entities is
returned, they are found in an `_embedded` attribute.

### Paging

Entity collections contain `page` elements detailing the current page size,
total entity count, total pages, and current page number (zero-indexed).
The response also contains `_links` for page navigation: `first`, `next`,
`previous`, `last` (and the current page, `self`).

### Search

Spring Data REST details search endpoints in the `<repository>/search` URL
(also found in the `._links.search.href` returned by `<repository>/`).

### Projections

Most entities have varying response structures, 'projections', which can
transform or include additional information from the platform. These are not
standardised, but can be explored through the Profile.

### Example

As an example, to find all services owned by the current user, with a page size of 2:

1. I GET `/secure/api/v2.0/users/current` and extract the `_links.self.href` value:
  * `https://f-tep.com/secure/api/v2.0/users/1`
2. I GET `/secure/api/v2.0/services/search` and identify the desired search function and its parameters:
  * `https://f-tep.com/secure/api/v2.0/services/search/findByOwner{?owner,page,size,sort,projection}`
3. I construct my search GET request URL with the user's `self` URL and desired page size:
  * `https://f-tep.com/secure/api/v2.0/services/search/findByOwner?owner=https://f-tep.com/secure/api/v2.0/users/1&size=2`

The response contains the two services, with their various attributes and
`_links`, as well as `page` info and navigation `_links` for the search results.
The (abbreviated) example response:

```json
{
  "_embedded": {
    "services": [
      {
        "name": "LandCoverS2OLD",
        "id": 7,
        "_links": {
          "self": {
            "href": "https://f-tep.com/secure/api/v2.0/services/7"
          }
        }
      },
      {

        "name": "QGIS",
        "id": 3,
        "_links": {
          "self": {
            "href": "https://f-tep.com/secure/api/v2.0/services/3"
          }
        }
      }
    ]
  },
  "_links": {
    "first": {
      "href": "https://f-tep.com/secure/api/v2.0/services/search/findByOwner?owner=https://f-tep.com/secure/api/v2.0/users/1&page=0&size=2"
    },
    "self": {
      "href": "https://f-tep.com/secure/api/v2.0/services/search/findByOwner?owner=https://f-tep.com/secure/api/v2.0/users/1&page=0&size=2"
    },
    "next": {
      "href": "https://f-tep.com/secure/api/v2.0/services/search/findByOwner?owner=https://f-tep.com/secure/api/v2.0/users/1&page=1&size=2"
    },
    "last": {
      "href": "https://f-tep.com/secure/api/v2.0/services/search/findByOwner?owner=https://f-tep.com/secure/api/v2.0/users/1&page=3&size=2"
    }
  },
  "page": {
    "size": 2,
    "totalElements": 8,
    "totalPages": 4,
    "number": 0
  }
}
```
