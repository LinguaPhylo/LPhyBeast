# Developer Guide - Test Instruction for LPhyBEAST

LPhyBEAST requires Java 17.

To test pre-released LPhyBEAST, ensure that all required LPhy and BEAST 2 packages are installed beforehand.
If you are using pre-release versions, follow the separate instructions for installing them.
After installation, print out the package versions to verify that you are testing against the correct ones.

## Install pre-released LPhyBEAST from Package Manager

1. Open "Package Manager" using BEAUti, click the button "Package Repository", 
and then click the button "Add URL" to add the following URL :

https://raw.githubusercontent.com/LinguaPhylo/LPhyBeastTest/main/beast2/prerelease.xml

It should look like the screenshot below:

<a href="./LPhyBeastTestRepo.png"><img src="LPhyBeastTestRepo.png" align="left" width="300" ></a>

After "Close" the window, you will see the version of LPhyBEAST refreshed. 
For example, we are testing 1.3.0 and the latest release is 1.2.1.

<a href="./PackageManager.png"><img src="PackageManager.png" align="left" width="600" ></a>

Click the button "Install/Upgrade" to install the pre-release.


2. Follow the user manual to install pre-released LPhy, and set up the `lphybeast` script

https://linguaphylo.github.io/setup/


3. Run the lphybeast script to verify that LPHY_LIB points to the correct directory.

Note: If it does not, remove any other LPhy versions that could be interfering with the path.

For example, 

```bash
$YOUR_PATH$/bin/lphybeast -V
```

```
openjdk version "17.0.5" 2022-10-18 LTS
...
LPHY_LIB = /Applications/lphystudio-1.7.0/lib
```

