import gitlabContainer.*

tasks.register("createContainer", CreateContainerTask::class)

tasks.register("createUser", CreateUserTask::class)

tasks.register("createAll") {
    group = "gitlab-container"
    dependsOn("createContainer", "createUser")
}

tasks.register("startContainer", StartContainerTask::class)

tasks.register("stopContainer", StopContainerTask::class)

tasks.register("removeContainer", RemoveContainerTask::class)

tasks.register("removeContainerData", RemoveContainerDataTask::class)

tasks.register("removeAll") {
    group = "gitlab-container"
    dependsOn("removeContainer", "removeContainerData")
}
