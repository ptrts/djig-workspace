import app.CloneAdjacentGitRepoTask

// todo cloneExampleApp, когда example-app переедет из репозитория workspace в свой отдельный репозиторий

tasks.register("cloneExampleDynamicApi", CloneAdjacentGitRepoTask::class, "example/dynamic-api", "example-dynamic-api").configure {
    group = "workspace"
}

tasks.register("cloneExampleDynamicDev", CloneAdjacentGitRepoTask::class, "example/dynamic-dev", "example-dynamic-dev").configure {
    group = "workspace"
}

tasks.register("cloneCore", CloneAdjacentGitRepoTask::class, "core").configure {
    group = "workspace"
}

tasks.register("cloneDynamicApi", CloneAdjacentGitRepoTask::class, "dynamic-api").configure {
    group = "workspace"
}

tasks.register("cloneEverything") {
    group = "workspace"
    dependsOn("cloneExampleDynamicApi", "cloneExampleDynamicDev", "cloneCore", "cloneDynamicApi")
}

tasks.register("initProject") {
    group = "app"

    description = """
    Initializes everything for the project. 
    This is an aggregator task for initExampleLocalGitLab and cloneEverything.
    """.trimIndent()

    dependsOn("gitLabContainerCreateAllWithProjects", "cloneEverything")
}
