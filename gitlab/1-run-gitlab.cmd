@echo off

if not exist 1-run-gitlab.sh (
    @echo The working directory must be the gitlab directory in the project
    exit 1
)

set home=%cd%\home

docker run ^
	--detach ^
	--hostname gitlab.domain.name.placeholder ^
	--publish 9522:22 ^
	--publish 9580:80 ^
	--publish 9543:443 ^
	--name gitlab.taruts.net ^
	--restart always ^
	--volume %home%/config:/etc/gitlab ^
	--volume %home%/logs:/var/log/gitlab ^
	--volume %home%/data:/var/opt/gitlab ^
	--shm-size 256m ^
	gitlab/gitlab-ee:latest
