#!/usr/bin/env bash

export DEBIAN_FRONTEND=noninteractive

sudo apt-get -y update
sudo apt-get -y dist-upgrade
sudo apt-get -y install --no-install-recommends apt-transport-https ca-certificates curl gnupg-agent software-properties-common nfs-common

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"

sudo apt-get -y update
sudo apt-get -y install --no-install-recommends docker-ce docker-ce-cli containerd.io
