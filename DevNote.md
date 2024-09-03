# Developer Guide

## After LPhyBEAST core changed

1. How to update dependencies in Intellij, especially SNAPSHOT version.
https://github.com/LinguaPhylo/LPhyBeast/blob/master/DEV_NOTE.md#update-dependencies-in-intellij

2. How to refresh the LPhyBEAST dependency:

<a href="./InstallLPhyBeast.png"><img src="InstallLPhyBeast.png" align="right" height="300" ></a>

LPhyBEAST is released as a ".zip" file into the Maven repository, 
because of the requirement of BEAST package framework. 
Therefore, it is not straight forwards as other dependencies, 
when you want to upgrade it to a newer version. 
You need to run the task `installLPhyBEAST` inside the build of `phylonco-lphybeast` subproject twice, 
at the first time it downloads the zip file and unzip it, 
at the second time it loads all jar files into the system. 
This process can be done either from command line below, 
or IntelliJ (screenshot on the right side).

```bash
./gradlew lphybeast-ext:installLPhyBEAST
```

3. Build lphybeast-ext subproject. If IntelliJ cannot build, please use the command line below to build the 
subproject `lphybeast-ext` first:

```bash
./gradlew :lphybeast-ext:build
```

## To be compatible with BEAST 2.7.x framework

Either running application or unit tests from IDE or Gradle tasks 
must load the "version.xml" file, which defines the services 
to be added into BEASTClassLoader.

So the "version.xml" file in the project root is used for release, 
but those in each sub-project folders are used for unit tests only.

The BEAST2 packages in the dependencies have to be installed, 
which will provide the "version.xml" file. 
The set of jar files in the "lib" folder are used for IDE development.
