# LPhyBeast and its extensions 

**Please note:** LPhy and LPhyBeast have moved to Maven project recently.
The developer guide is currently under revision.

-----------------------

TODO:

LPhyBeast and its extensions are developed as [BEAST2 packages](https://www.beast2.org/managing-packages/).
More details are available for developers at https://linguaphylo.github.io/developer/.

## Maven & development environment

Install [OpenJDK 17](https://jdk.java.net/17/) and 

You can look at [Setup development environment](https://linguaphylo.github.io/developer/setup-dev-env/)
and [LPhy developer note](https://github.com/LinguaPhylo/linguaPhylo/blob/master/DEV_NOTE.md) for more details.

## Dependencies 

LPhyBeast depends on LPhy and [few BEAST 2 packages](version.xml). 
Its extensions depend on LPhyBeast, corresponding LPhy extensions and corresponding BEAST 2 packages.
A case study is available at https://linguaphylo.github.io/developer/dependencies/.

## Release procedure


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

You can setup the XML to declare package repositories in your Github, 
e.g., [prerelease.xml](https://raw.githubusercontent.com/LinguaPhylo/LPhyBeastTest/main/beast2.7/lib/prerelease.xml) 
in LPhyBeastTest. Then add this URL into the package repositories though 
Package Manager => Package repositories => Add URL. Remember to delete it after the installation is done.

## Useful Links

- [LPhy developer note](https://github.com/LinguaPhylo/linguaPhylo/blob/master/DEV_NOTE.md)

- [Testing pipeline for tutorials](https://github.com/LinguaPhylo/LPhyBeastTest)

- [beast-phylonco](https://github.com/bioDS/beast-phylonco)

