# REST API Authentication

F-TEP is secured with ESA's [EO-SSO](https://eo-sso-idp.eo.esa.int/) service.
As a SAML 2.0 (Shibboleth) Service Provider, a good overview of the
authentication process can be found at [Wikipedia](https://en.wikipedia.org/wiki/Security_Assertion_Markup_Language#Use).

The exact implementation of an authentication flow in a given client is
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
