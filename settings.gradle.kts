
rootProject.name = "LPhyBeast"

include("lphybeast")
include("lblauncher")

// https://docs.gradle.org/current/userguide/build_cache.html
// https://docs.gradle.org/current/userguide/build_cache_use_cases.html
//buildCache {
//    local {
//        directory = File(rootDir, "build-cache")
//        removeUnusedEntriesAfterDays = 30
//        println("Creating local build cache : ${directory}")
//    }
//}
