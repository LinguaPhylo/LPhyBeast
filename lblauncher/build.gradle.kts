plugins {
    `java-library`
    `maven-publish`
    signing
    id("io.github.linguaphylo.platforms.lphy-publish") version "0.1.2"
}

version = "0.0.1-SNAPSHOT"
base.archivesName.set("lb-launcher")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("io.github.linguaphylo:lphy:1.3.1")
    implementation("io.github.linguaphylo:lphy-studio:1.3.1")

    implementation(project(":lphybeast")) // not depend on LPhyBeast, only use for debug
    // BEAST launcher
    implementation(fileTree("lib"))

}

val maincls : String = "lphybeast.launcher.LPhyBeastLauncher"

val developers = "LPhyBEAST developer team"
tasks.jar {
    manifest {
        // shared attr in the root build
        attributes(
            "Main-Class" to maincls,
            "Implementation-Title" to "LPhyBEAST Launcher",
            "Implementation-Vendor" to developers,
        )
    }
}

val webSteam = "github.com/LinguaPhylo/LPhyBeast"
publishing {
    publications {
        // project.name contains "lphy" substring
        create<MavenPublication>(project.name) {
            artifactId = project.base.archivesName.get()
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
                    "The GUI to run LPhyBeast."
                )
                // compulsory
                url.set("https://linguaphylo.github.io/")
                packaging = "jar"
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


