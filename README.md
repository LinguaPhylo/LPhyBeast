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
But you need to add the extra repository
`https://raw.githubusercontent.com/CompEvol/CBAN/master/packages-extra.xml`
to the Package Manager to view it.
Follow the [instruction](https://linguaphylo.github.io/setup/) to set up,
and then use the script `lphybeast` to create the XML:

A simple usage of LPhyBEAST to create `RSV2.xml` given 
the LPhy script [RSV2.lphy](https://github.com/LinguaPhylo/linguaPhylo/blob/master/tutorials/RSV2.lphy).

```bash
$BEAST_FOLDER/bin/lphybeast tutorials/RSV2.lphy
```

Create 10 BEAST 2 XMLs for a simple 
[HKY+Coalescent](https://github.com/LinguaPhylo/linguaPhylo/blob/master/examples/hkyCoalescent.lphy) 
simulation study:


```bash
$BEAST_FOLDER/bin/lphybeast -r 10 examples/hkyCoalescent
```

More scripts are available in 
[linguaPhylo/examples](https://github.com/LinguaPhylo/linguaPhylo/tree/master/examples).

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

The details are in [version.xml](./version.xml). All released BEAST 2 packages are listed in
[Package Viewer](https://compevol.github.io/CBAN/).

BEASTLabs `beast.util.Script` depends on `jdk.nashorn.api.scripting.ScriptObjectMirror`.
If there is `NoClassDefFoundError` for it, you can add "-Xbootclasspath/a:${nashorn_path}" to your javac, 
where `${nashorn_path}=/my/path/to/libext/nashorn.jar`.

## LPhyBeastExt
The BEAST 2 package extended from the core project [LPhyBeast](https://github.com/LinguaPhylo/LPhyBeast), which includes:

- Mascot LPhyBeast extension;
- MM LPhyBeast extension;
- SA LPhyBeast extension.

## Useful Links

- [Developer note](DEV_NOTE.md)
