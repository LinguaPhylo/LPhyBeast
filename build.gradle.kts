import java.text.SimpleDateFormat
import java.util.Calendar

// Configures this project and each of its sub-projects.
allprojects {
    repositories {
        mavenCentral()
//        mavenLocal() // only for testing
    }
}

// Configures the sub-projects of this project.
subprojects {
//    tasks.withType<JavaCompile> {
//        options.isWarnings = true
//    }

    var calendar: Calendar? = Calendar.getInstance()
    var formatter = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")

    // shared attributes
    tasks.withType<Jar>() {
        manifest {
            attributes(
                "Implementation-Vendor" to "LPhy team",
                "Implementation-Version" to archiveVersion,
                "Implementation-URL" to "https://github.com/LinguaPhylo/linguaPhylo",
                "Built-By" to "Walter Xie", //System.getProperty("user.name"),
                "Build-Jdk" to JavaVersion.current().majorVersion.toInt(),
                "Built-Date" to formatter.format(calendar?.time)
            )
        }
    }

}

