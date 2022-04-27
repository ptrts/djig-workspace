#!/bin/sh

if [ ! -f 1-run-gitlab.sh ]; then
    echo "The working directory must be the gitlab directory in the project"
    exit 1
fi

thisDirectory="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 || exit 1 ; pwd -P )"
home=$thisDirectory/home

docker run \
  --detach \
  --hostname gitlab.domain.name.placeholder \
  --publish 9522:22 \
  --publish 9580:80 \
  --publish 9543:443 \
  --name gitlab.taruts.net \
  --restart always \
  --volume $home/config:/etc/gitlab \
  --volume $home/logs:/var/log/gitlab \
  --volume $home/data:/var/opt/gitlab \
  --shm-size 256m \
  --add-host host.docker.internal:host-gateway \
  gitlab/gitlab-ee:latest
