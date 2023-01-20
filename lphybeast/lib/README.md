# BEAST 2.7.x mechanism 

https://github.com/CompEvol/BeastFX/blob/master/DevGuideIntelliJ.md

https://www.beast2.org/2022/12/19/what-is-new-in-v2.7.3.html

1. Load version.xml

It defines the services to be registered into BEASTClassLoader.
Using IDE, every version.xml in the packages depending on BEAST 2.7 
have to be loaded by the development version.

2. Unit test preparation

- in local machine: use Package Manager to install all dependencies under your local package repository folder;
- in Github: use workflow to call Package Manager to install all dependencies.
