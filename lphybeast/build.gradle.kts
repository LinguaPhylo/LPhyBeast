plugins {
    `java-library`
    `java-test-fixtures` // which produces test fixtures
// DO NOT use app plugin, it will mess up distZip
    distribution
    `maven-publish`
    signing
    id("io.github.linguaphylo.platforms.lphy-publish") version "0.1.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

val beast2Jars = fileTree("lib") {
//    exclude("**/starbeast2-*.jar")
    exclude("**/*src.jar")
    exclude("**/*javadoc.jar")
}
//val notReleasedJars = fileTree("lib") {
//    include("lphy-1.3.*.jar")
//}
val lblibs by configurations.creating {
    // Add to defaultDependencies to get their all jars
    defaultDependencies {
        add(project.dependencies.api("com.google.guava:guava:23.6-jre")) // required by LoggerHelper
        add(project.dependencies.implementation("org.jblas:jblas:1.2.3"))
        add(project.dependencies.implementation("info.picocli:picocli:4.7.0"))
    }
}

// TODO 3 versions: here, LPhyBeastCMD, version.xml
// version has to be manually adjusted to keep same between version.xml and here
version = "1.1.0"//-SNAPSHOT


// if the project dependencies ues impl, then impl(proj(..)) will only have source code,
// which is equivalent to project-version.jar.
// if api is used, then all dependencies will pass to here,
// but do not use api unless you have to.
dependencies {
//    implementation(lblibs)

    /**
     * The behaviour of this default version declaration chooses any available highest version first.
     * If the exact version is required, then use the "strictly version" declaration
     * such as "io.github.linguaphylo:lphy:1.2.0!!".
     * https://docs.gradle.org/current/userguide/rich_versions.html#sec:strict-version
     */
    // no API change in 1.4.4
    api("io.github.linguaphylo:lphy:1.5.0-SNAPSHOT") //-SNAPSHOT
    api("io.github.linguaphylo:lphy-base:1.5.0-SNAPSHOT")

    // all released beast 2 libs
    // TODO beast2 jar contains Apache math. Be aware of version conflict to LPhy dependency.
    // TODO better way to load version.xml?
    api(beast2Jars)
    // other jars must be included
//    implementation(notReleasedJars)
//    if (project.hasProperty("isRuntime")) {
//        runtimeOnly("io.github.linguaphylo:lphy:1.1.0")
//        runtimeOnly(beast2Jars)
//    }

    // Tests are the bottom of the file
}

val maincls = "lphybeast.LPhyBeastCMD"
val developers = "LPhyBEAST developer team"
tasks.jar {
    manifest {
        // shared attr in the root build
        attributes(
            "Main-Class" to maincls,
            "Implementation-Title" to "LPhyBEAST",
            "Implementation-Vendor" to developers,
        )
    }
}

// launch lphybeast
tasks.register("runLPhyBEAST", JavaExec::class.java) {
    // use classpath
    jvmArgs = listOf("-cp", sourceSets.main.get().runtimeClasspath.asPath)
    println("clspath = ${sourceSets.main.get().runtimeClasspath.asPath}")
    mainClass.set(maincls)
    setArgs(listOf("-o", "$rootDir/tmp/RSV2.xml",
        "$rootDir/../linguaPhylo/examples/RSV2.lphy"))
}

tasks.getByName<Tar>("distTar").enabled = false
// exclude start scripts
//tasks.getByName<CreateStartScripts>("startScripts").enabled = false

// dist as a beast2 package:
// 1. never use `application` Gradle plugin;
// 2. include the task output, not the output files;
// 3. exclude lphy from lphybeast core release, because SPI does not work with BEAST class loader.
//    But for lphybeast extensions, lphy part has to be included, due to BEAST class loader.
distributions {
    main {
        contents {
//            eachFile {  println(relativePath)  }
            includeEmptyDirs = false
            into("lib") {
//                println(lblibs.files.toList())
//                println(lblibs.fileCollection(project.dependencies.implementation("com.google.guava:guava:23.6-jre")).toList())
                // all 3rd party jars
                from(lblibs.files)
                // lphybeast core jar
                from(tasks.jar)
            }
            into("."){
                from("$rootDir") {
                    include("README.md")
                    include("LICENSE")
                    include("version.xml")
                }
            }
            // include src jar
            into("src") {
                from(tasks.getByName<Jar>("sourcesJar"))
            }
            // lphybeast script
            from("${layout.projectDirectory.dir("bin")}") {
                include("lphybeast")
                into("bin")
                eachFile {
                    // fileMode 755 not working
                    file.setExecutable(true, true)
                }
            }
        }
    }
}

// beast 2 will remove version from Zip file name, and then decompress
// rm lphybeast-$version from the relative path of files inside Zip to make it working
tasks.withType<Zip>() {
    doFirst {
        if ( name.equals("distZip") ) {
            // only activate in distZip, otherwise will affect all jars and zips,
            // e.g. main class not found in lphybeast-$version.jar.
            eachFile {
                relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                println(relativePath)
            }
        }
    }
}

val webSteam = "github.com/LinguaPhylo/LPhyBeast"
publishing {
    publications {
        // project.name contains "lphy" substring
        create<MavenPublication>(project.name) {
            artifactId = project.base.archivesName.get()
            artifact(tasks.distZip.get())
            artifact(tasks["testFixturesJar"])
            // Configures the version mapping strategy
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set(project.name)
                description.set(
                    "A command-line program that takes an LPhy model specification " +
                            "including a data block, and produces a BEAST 2 XML input file."
                )
                // compulsory
                url.set("https://linguaphylo.github.io/")
                packaging = "zip"
                developers {
                    developer {
                        name.set(developers)
                    }
                }
                properties.set(
                    mapOf(
                        "maven.compiler.source" to java.sourceCompatibility.majorVersion,
                        "maven.compiler.target" to java.targetCompatibility.majorVersion
                    )
                )
                licenses {
                    license {
                        name.set("GNU Lesser General Public License, version 3")
                        url.set("https://www.gnu.org/licenses/lgpl-3.0.txt")
                    }
                }
                // https://central.sonatype.org/publish/requirements/
                scm {
                    connection.set("scm:git:git://${webSteam}.git")
                    developerConnection.set("scm:git:ssh://${webSteam}.git")
                    url.set("https://${webSteam}")
                }
            }
            println("Define MavenPublication ${name} and set shared contents in POM")
        }
    }
}

//++++++++ tests ++++++++//

dependencies {
//    intTestImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // API dependencies are visible to consumers when building
    testFixturesApi("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation(testFixtures(project(":lphybeast")))

//    testRuntimeOnly("io.github.linguaphylo:lphy:1.1.0")
//    testRuntimeOnly(beast2Jars)
}

tasks.test {
    useJUnitPlatform() {
        excludeTags("dev")
    }
    // set heap size for the test JVM(s)
    minHeapSize = "128m"
    maxHeapSize = "1G"
    // show standard out and standard error of the test JVM(s) on the console
    testLogging.showStandardStreams = true

    reports {
        junitXml.apply {
            isOutputPerTestCase = true // defaults to false
            mergeReruns.set(true) // defaults to false
        }
    }

}

//val testTutorials = task<Test>("testTutorials") {
//    description = "Test tutorials."
//    group = "tutorials"
//
//    include("**/H5N1TutorialTest.class")
//}