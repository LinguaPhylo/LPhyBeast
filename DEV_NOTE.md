# LPhyBeast and its extensions 

LPhyBeast and its extensions are developed as [BEAST2 packages](https://www.beast2.org/managing-packages/).
More details are available for developers at https://linguaphylo.github.io/developer/.

## Gradle & development environment

Install [OpenJDK 17](https://jdk.java.net/17/) and [Gradle 7.x](https://gradle.org/install/). 

You can look at [Setup development environment](https://linguaphylo.github.io/developer/setup-dev-env/)
and [LPhy developer note](https://github.com/LinguaPhylo/linguaPhylo/blob/master/DEV_NOTE.md) for more details.

## Dependencies 

LPhyBeast depends on LPhy and [few BEAST 2 packages](version.xml). 
Its extensions depend on LPhyBeast, corresponding LPhy extensions and corresponding BEAST 2 packages.
A case study is available at https://linguaphylo.github.io/developer/dependencies/.

The Gradle project is using more restricted configuration between different projects,
which requires to read the source code from the released jars.
Please read [the difference between libraries and applications](https://docs.gradle.org/current/userguide/library_vs_application.html).

### Dependencies to zip files

BEAST 2 packages are released as zip files. LPhyBeast and [LPhyBeastExt](https://github.com/LinguaPhylo/LPhyBeastExt)
are published to [Maven linguaphylo group](https://search.maven.org/search?q=io.github.linguaphylo).
Using the predefined [Gradle function](https://github.com/LinguaPhylo/LPhyBeastExt/blob/a31263ef418c63596515eb2ee1b308046423184e/lphybeast-ext/build.gradle.kts#L21-L56), 
the zip file can be downloaded and unzipped automatically, and then jars will be loaded into the library. 

## Release procedure

1. Check all versions, for snapshot, make sure they contain the postfix "SNAPSHOT".
   Otherwise, for final release, make sure every postfix "SNAPSHOT" is removed.
   Run the command below in the terminal, which includes all unit tests.

```bash
./gradlew clean build --no-build-cache
```

Please note if the zip file includes multiple jars from different subprojects (e.g. LPhyBeastExt), 
then you need to run the step 2. Check the message to see if the jars were included. 

2. Run the command below, only when the zip file includes multiple jars.
   The zip file looking like `lphybeast-x.x.x.zip` will be available at
   `$PROJECT_DIR/lphybeast/distributions`.

```bash
./gradlew build -x test
```

Tips: for extensions, you can create an empty subproject to handle the lphybeast core,
to reduce the complexity of dependencies in other subproject(s).
For example https://github.com/LinguaPhylo/LPhyBeastExt/tree/master/lphybeast-ext.

3. Run the (1 line) command below to publish to the Maven central repository. 

```bash
./gradlew publish --info 
    -Psigning.secretKeyRingFile=/path/to/.gnupg/mysecret.gpg 
    -Psigning.password=mypswd -Psigning.keyId=last8symbols 
    -Possrh.user=myuser -Possrh.pswd=mypswd
```

Please note: if not snapshot, once it is published, you will not be able to remove/update/modify the release.

4. For final release, follow the [instruction](https://central.sonatype.org/publish/release/) to manually
   complete the releasing deployment.

For snapshots, check https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/linguaphylo/.
For releases, check https://s01.oss.sonatype.org/content/repositories/releases/io/github/linguaphylo/.

## Test

We recommend you copy
- [these unit tests](https://github.com/LinguaPhylo/LPhyBeast/tree/master/lphybeast/src/test/java/lphybeast)
to check the XMLs.
- [these integration tests for tutorials](https://github.com/LinguaPhylo/LPhyBeastTest) 
to check the BEAST2 runs and the logs.

Two-stage tests are also recommended, the first stage tests the pre-released version (e.g. snapshot),
in order to find any bugs before the final release.
This can be done programmatically by setting the customised BEAST2 package directory `-Dbeast.user.package.dir=...`.
The second stage repeats the same process but tests the final release.
