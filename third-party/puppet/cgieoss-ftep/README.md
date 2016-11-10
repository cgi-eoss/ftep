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
* `ftep::portal` (TBD)
* `ftep::backend` (in progress)

Configuration parameters shared by these classes may be set via `ftep::globals`.

## Limitations

This module currently only targets installation on CentOS 6 nodes.
