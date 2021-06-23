#!/usr/bin/env bash

export DEBIAN_FRONTEND=noninteractive

while ! sudo apt -y update; do
  sleep 1
done

sudo apt -y dist-upgrade
sudo apt -y install --no-install-recommends apt-transport-https ca-certificates curl gpg-agent software-properties-common nfs-common

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"

sudo apt -y update
sudo apt -y install --no-install-recommends docker-ce docker-ce-cli containerd.io
