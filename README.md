# LPhyBeast

[![Build Status](https://github.com/LinguaPhylo/LPhyBeast/workflows/Lphy%20BEAST%20tests/badge.svg)](https://github.com/LinguaPhylo/LPhyBeast/actions?query=workflow%3A%22Lphy+BEAST+tests%22)


LPhyBEAST is a command-line program that takes a
[LPhy](http://linguaphylo.github.io/) script file including
model specification and data block, 
and produces a [BEAST 2](http://beast2.org/) XML file. 
It therefore enables LPhy as an alternative way to succinctly
express and communicate BEAST2 analyses.

## Setup and usage

LPhyBEAST is implemented as an application in the BEAST 2 package "lphybeast". 
The installation guide and usage instructions are available in the [User Manual](https://linguaphylo.github.io/setup/#lphybeast-installation). 


## Tutorials

LPhy and LPhyBEAST [Tutorials](https://linguaphylo.github.io/tutorials/)


## Dependencies

- [linguaPhylo](https://github.com/LinguaPhylo/linguaPhylo)

BEAST 2 packages, for example:

- [beast2](http://www.github.com/CompEvol/beast2)
- [BEASTLabs](https://github.com/BEAST2-Dev/BEASTLabs/)
- [feast](https://github.com/BEAST2-Dev/BEASTLabs/)
- [BEAST_CLASSIC](https://github.com/BEAST2-Dev/beast-classic/)
- [SMM](https://github.com/BEAST2-Dev/substmodels/)

The details are in [version.xml](./lphybeast/version.xml). All released BEAST 2 packages are listed in
[Package Viewer](https://compevol.github.io/CBAN/).

BEASTLabs `beast.util.Script` depends on `jdk.nashorn.api.scripting.ScriptObjectMirror`.
If there is `NoClassDefFoundError` for it, you can add "-Xbootclasspath/a:${nashorn_path}" to your javac, 
where `${nashorn_path}=/my/path/to/libext/nashorn.jar`.

## LPhyBeastExt

More in another BEAST 2 package to host [LPhyBeast extensions](lphybeast-ext-dist/README.md).

## Useful Links

- [Developer note](DEV_NOTE.md)
