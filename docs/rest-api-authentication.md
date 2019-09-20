# REST API Authentication

F-TEP is secured with ESA's [EO-SSO](https://eo-sso-idp.eo.esa.int/) service.
As a SAML 2.0 (Shibboleth) Service Provider, a good overview of the
authentication process can be found at [Wikipedia](https://en.wikipedia.org/wiki/Security_Assertion_Markup_Language#Use).

## F-TEP API Keys

To simplify authentication for automated workflows, F-TEP offers a key-based
authentication system for users. To use this feature:

1. Access your 'My Account' page.
2. Under 'API key management', click the 'Generate' button
3. **IMPORTANT:** Save the generated key. It will never be displayed again!

In case of a forgotten or lost API key, the 'Regenerate' button will replace
the current key with a new one.

The API key can be used in place of the password in a standard HTTP Basic
workflow. For example, using curl's `--user` parameter:

```
$ curl --user 'myusername:MYSECRETAPIKEY' "https://f-tep.com/secure/api/v2.0/users/current"
{
  "id" : 9999,
  "name" : "myusername",
  "email" : "my.email@example.com",
  "role" : "USER",
  "organisation" : null,
  "_links" : {
    "self" : {
      "href" : "https://f-tep.com/secure/api/v2.0/users/9999"
    },
    "user" : {
      "href" : "https://f-tep.com/secure/api/v2.0/users/9999{?projection}",
      "templated" : true
    },
    "wallet" : {
      "href" : "https://f-tep.com:8080/secure/api/v2.0/wallets/9999{?projection}",
      "templated" : true
    }
  }
}
```

## EO-SSO

The exact implementation of a SAML 2.0 authentication flow in a given client is
extremely client-specific. Many third-party tools and libraries are available.
Some functions or code from the following projects may be useful for your
application:

* *Python*:
  * https://github.com/onelogin/python-saml
* *Java*:
  * https://github.com/DARIAH-DE/shib-http-client

Additionally, some ideas from the "Enhanced Client or Proxy" SAML concept may
be used. The Shibboleth wiki includes a [list of links to known ECP
implementations](https://wiki.shibboleth.net/confluence/display/CONCEPT/ECP).
