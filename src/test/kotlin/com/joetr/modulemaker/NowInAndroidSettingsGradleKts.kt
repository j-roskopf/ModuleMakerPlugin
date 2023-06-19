package com.joetr.modulemaker

object NowInAndroidSettingsGradleKts {
    val data = """
        /*
         * Copyright 2021 The Android Open Source Project
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     https://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */

        pluginManagement {
            includeBuild("build-logic")
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
            }
        }

        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                google()
                mavenCentral()
            }
        }
        rootProject.name = "nowinandroid"
        include(":app")
        include(":app-nia-catalog")
        include(":benchmarks")
        include(":core:common")
        include(":core:data")
        include(":core:data-test")
        include(":core:database")
        include(":core:datastore")
        include(":core:datastore-test")
        include(":core:designsystem")
        include(":core:domain")
        include(":core:model")
        include(":core:network")
        include(":core:ui")
        include(":core:testing")
        include(":core:analytics")
        include(":core:notifications")

        include(":feature:foryou")
        include(":feature:interests")
        include(":feature:bookmarks")
        include(":feature:topic")
        include(":feature:settings")
        include(":lint")
        include(":sync:work")
        include(":sync:sync-test")
        include(":ui-test-hilt-manifest")
    """.trimIndent()

    val filePathData = """
        /*
         * Copyright 2021 The Android Open Source Project
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     https://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */

        pluginManagement {
            includeBuild("build-logic")
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
            }
        }

        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                google()
                mavenCentral()
            }
        }
        rootProject.name = "nowinandroid"
        include(":app", "path/to")
        include(":app-nia-catalog", "path/to")
        include(":benchmarks", "path/to")
        include(":core:common", "path/to")
        include(":core:data", "path/to")
        include(":core:data-test", "path/to")
        include(":core:database", "path/to")
        include(":core:datastore", "path/to")
        include(":core:datastore-test", "path/to")
        include(":core:designsystem", "path/to")
        include(":core:domain", "path/to")
        include(":core:model", "path/to")
        include(":core:network", "path/to")
        include(":core:ui", "path/to")
        include(":core:testing", "path/to")
        include(":core:analytics", "path/to")
        include(":core:notifications", "path/to)"

        include(":feature:foryou", "path/to")
        include(":feature:interests", "path/to")
        include(":feature:bookmarks", "path/to")
        include(":feature:topic", "path/to")
        include(":feature:settings", "path/to")
        include(":lint", "path/to")
        include(":sync:work", "path/to")
        include(":sync:sync-test", "path/to")
        include(":ui-test-hilt-manifest", "path/to")
    """.trimIndent()
}
