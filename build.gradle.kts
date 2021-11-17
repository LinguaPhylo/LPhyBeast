import java.text.SimpleDateFormat
import java.util.Calendar

// Configures this project and each of its sub-projects.
allprojects {
    repositories {
        mavenCentral()
        // add sonatype snapshots repository
        maven {
            url=uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
//        mavenLocal() // only for testing
    }
}

// Configures the sub-projects of this project.
subprojects {
    group = "io.github.linguaphylo"

    var calendar: Calendar? = Calendar.getInstance()
    var formatter = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")

    // shared attributes
    tasks.withType<Jar>() {
        manifest {
            attributes(
                "Implementation-Vendor" to "LPhy team",
                "Implementation-Version" to archiveVersion,
                "Implementation-URL" to "https://github.com/LinguaPhylo/LPhyBeast/",
                "Built-By" to "Walter Xie", //System.getProperty("user.name"),
                "Build-Jdk" to JavaVersion.current().majorVersion.toInt(),
                "Built-Date" to formatter.format(calendar?.time)
            )
        }
        // copy LICENSE to META-INF
        metaInf {
            from (rootDir) {
                include("LICENSE")
            }
        }
    }
}

