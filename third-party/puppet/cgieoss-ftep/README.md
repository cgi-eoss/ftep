# ftep

#### Table of Contents

1. [Description](#description)
1. [Setup - The basics of getting started with ftep](#setup)
    * [Setup requirements](#setup-requirements)
    * [Beginning with ftep](#beginning-with-ftep)
1. [Usage - Configuration options and additional functionality](#usage)
1. [Reference - An under-the-hood peek at what the module is doing and how](#reference)
1. [Limitations - OS compatibility, etc.](#limitations)
1. [Development - Guide for contributing to the module](#development)

## Description

The ftep module lets you use Puppet to install and configure the Forestry TEP
service infrastructure.

[F-TEP](https://github.com/cgi-eoss/f-tep) is an open platform for the forestry
community to access and exploit EO data. This Puppet module may be used to
set up the various components including the community hub, the F-TEP webapp,
and the processing manager.

**Note:** Currently this module is only compatible with CentOS 6.

**<span style="color:red;">Warning:</span>** This module is incomplete.

## Setup

### Setup Requirements

* This module may manage a yum repository for package installation with the
  parameter `ftep::repo::location`. This may be the URL of a hosted repo, or
  an on-disk path to a static repo (e.g. built with `createrepo`) in the format
  `file:///path/to/fteprepo/$releasever/local/$basearch`. The latter is useful
  for standalone `puppet apply` deployments.

## Usage

The ftep module may be used to install the F-TEP components individually by the
classes:
* `ftep::db`
* `ftep::drupal`
* `ftep::geoserver`
* `ftep::monitor`
* `ftep::proxy`
* `ftep::resto`
* `ftep::server`
* `ftep::webapp`
* `ftep::worker`
* `ftep::wps`

Configuration parameters shared by these classes may be set via `ftep::globals`.

Interoperability between the components is managed via hostnames, which may be
resolved at runtime via DNS or manually, by overriding the `ftep::globals::hosts_override`
hash. See the `ftep::globals` class for available parameters, and the specific
component classes for how these are used, for example in `apache::vhost`
resources.

### Manual configuration actions

Some components of F-TEP are not fully instantiated by this Puppet module.
Following the automated provisioning of an F-TEP environment, some manual steps
must be carried out to ensure full functionality of some components. These may
be omitted when some functionality is not required.

The following list describes some of these possible post-installation actions:
* `ftep::drupal`: Drupal site initialisation &amp; content restoration
* `ftep::monitor`: Creation of graylog inputs &amp; dashboards
* `ftep::monitor`: Creation of grafana dashboards
* `ftep::worker`: Installation of downloader credentials
* `ftep::wps`: Restoration &amp; publishing of default F-TEP services


## Limitations

This module currently only targets installation on CentOS 6 nodes.
