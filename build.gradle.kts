import app.CloneAdjacentGitRepoTask
import app.InitLocalDynamicProjectsTask

// todo cloneExampleGitLabContainer. Когда проект example-gitlab-container переедет из репозитория workspace в отдельный репозиторий
//      В прочем, сие у нас даже не планируется. example-gitlab-container планируется преобразовать в Gradle плагин
//      Оно тогда и перестанет быть частью многопроектной Gradle сборки workspace
//      Скорее всего, у нас тогда не будет многопроектной Gradle сборки workspace вообще, workspace станет не более чем вязанкой группы
//      проектов, каждый из которых сидит в своем отдельном Git репозитории

// Таска создания example-dynamic-local из example-dynamic-dev
// Она у нас поставлена после поднятия локального контейнера GitLab,
// т.к. этот самый example-dynamic-local у нас будет запушен в локальный GitLab
val initExampleDynamicLocal = tasks.registering(InitLocalDynamicProjectsTask::class) {
    // The project example-dynamic-local is supposed to be stored in the local GitLab,
    // so the task creating example-dynamic-local must go after the task that creates the local GitLab.
    mustRunAfter(":example-gitlab-container:createAll")
}

// Таска, которая создает локальный GitLab контейнер и создает в нем проект example-dynamic-local
val initExampleLocalGitLab by tasks.registering {
    group = "app"

    description = """
    An aggregator task that creates a local GitLab Docker container, 
    creates a project in there which is a fork of example-dynamic-dev 
    and clones it into the example-dynamic-local project subdirectory
    """.trimIndent()

    dependsOn(":example-gitlab-container:createAll", initExampleDynamicLocal)
}

// We put all the cloning tasks after GitLab container tasks in order to fail fast.
// The GitLab container tasks are unstable and slow, they are the most probable point of failure here.

// todo cloneExampleApp, когда example-app переедет из репозитория workspace в свой отдельный репозиторий

tasks.register("cloneExampleDynamicApi", CloneAdjacentGitRepoTask::class, "example/dynamic-api").configure {
    mustRunAfter(initExampleLocalGitLab)
}

tasks.register("cloneExampleDynamicDev", CloneAdjacentGitRepoTask::class, "example/dynamic-dev").configure {
    mustRunAfter(initExampleLocalGitLab)
}

tasks.register("cloneCore", CloneAdjacentGitRepoTask::class, "core").configure {
    mustRunAfter(initExampleLocalGitLab)
}

tasks.register("cloneDynamicApi", CloneAdjacentGitRepoTask::class, "dynamic-api").configure {
    mustRunAfter(initExampleLocalGitLab)
}

tasks.register("initProject") {
    group = "app"

    description = """
    Initializes everything for the project. 
    This is an aggregator task for initExampleLocalGitLab, cloneExampleDynamicApi, cloneExampleDynamicDev, cloneCore and cloneDynamicApi
    """.trimIndent()

    dependsOn(initExampleLocalGitLab, "cloneExampleDynamicApi", "cloneExampleDynamicDev", "cloneCore", "cloneDynamicApi")
}
