# LPhyBEAST

## Project Overview

LPhyBEAST is a command-line program that converts [LPhy](https://linguaphylo.github.io/) script files (model specification + data block) into [BEAST 2](http://beast2.org/) XML input files. It is implemented as a BEAST 2 package (`lphybeast`) and acts as a bridge between the LPhy probabilistic model specification language and the BEAST 2 Bayesian phylogenetic inference engine.

- **GitHub**: https://github.com/LinguaPhylo/LPhyBeast
- **License**: LGPL-2.1
- **Language**: Java (98%)
- **Latest release**: v1.3.0

---

## Repository Structure

```
LPhyBeast/
├── lphybeast/            # Core module: LPhyBEAST BEAST 2 package
├── lphybeast-ext-dist/   # Extension distribution packaging
├── lphybeast-flc/        # Extension: Flexible Lewis coding models
├── lphybeast-mascot/     # Extension: MASCOT (multi-type coalescent)
├── lphybeast-mm/         # Extension: Mixture models
├── lphybeast-sa/         # Extension: Sampled ancestor models
├── lblauncher/           # Launcher scripts for LPhyBEAST CLI
├── scripts/              # Utility/build scripts
├── IntelliJ/.idea/       # IntelliJ IDEA project config
├── .github/workflows/    # CI/CD GitHub Actions
├── pom.xml               # Root Maven POM
├── DevNote.md            # Developer notes
├── NeSI.md               # NeSI HPC cluster notes
└── TestInstruction.md    # Testing instructions
```

The core logic lives in `lphybeast/`. The main entry point is `lphybeast/src/main/java/lphybeast/LPhyBeastCMD.java`.

---

## Technology Stack & Architecture

This project sits at the intersection of **three distinct technology groups**, each with different Java versions and extension mechanisms:

### 1. LPhy Group (Java 17 + JPMS + SPI)
- Uses Java 17, the Java Platform Module System (JPMS), and the standard Service Provider Interface (SPI)
- Extensions register via `module-info.java` and a container provider class under `mypackage.lphy.spi`
- Published to Maven Central under group `io.github.linguaphylo`

### 2. BEAST 2 Group (Java 1.8, no JPMS)
- BEAST 2 core uses Java 1.8 and its own class loader-based extension mechanism
- BEAST 2 packages declare dependencies in `version.xml`
- File dependencies (JARs in `lib/`) are used since BEAST 2 is not on Maven Central

### 3. LPhyBEAST Group (Java 17, no JPMS — the bridge)
- Provides mapping classes between LPhy and BEAST 2
- Uses Java 17 but NOT JPMS
- Released and managed as BEAST 2 packages (installable via BEAST 2 Package Manager)
- Requires LPhy libraries on the Java classpath to trigger SPI

---

## For Developer using CMD

**Maven** is the build tool. The root `pom.xml` is a multi-module POM aggregating all submodules.

### Build

Maven builds follow an ordered lifecycle. The key phases used here are `clean` (removes previous build output from `target/`), `compile` (compiles source code), `package` (bundles compiled code into a JAR), and `install` (installs the JAR into your local `~/.m2` repository so other modules can depend on it). Running `install` automatically executes all earlier phases in sequence.

The `-pl <module>` flag (short for `--projects`) selects a specific submodule by directory name. The `-am` flag (short for `--also-make`) additionally builds any local modules that the selected module depends on — useful when upstream changes haven't been installed yet. The `-DskipTests` property skips test execution to speed up the build.

```bash
# Clean previous build output and install all modules into the local Maven repo
mvn clean install

# Build and install only the core lphybeast module (assumes dependencies are already installed)
mvn -pl lphybeast clean install

# Build lphybeast together with all local modules it depends on (e.g. after changing lphy sources)
mvn -pl lphybeast -am clean install

# Build all modules quickly, skipping test execution
mvn clean install -DskipTests
```

**Example** — first-time setup, building the whole project from scratch:
```bash
git clone https://github.com/LinguaPhylo/LPhyBeast.git
cd LPhyBeast
mvn clean install -DskipTests
```

### Test

The `test` phase compiles test sources and runs all unit tests via the Maven Surefire plugin. It does not require the project to be packaged. The `-Dtest=<ClassName>` property restricts execution to a single test class. The `-pl <module>` flag scopes the run to one submodule.

```bash
# Run all tests across all modules from the project root
mvn test

# Run tests for the core lphybeast module only
mvn -pl lphybeast test

# Run a single test class in the lphybeast module
mvn -pl lphybeast test -Dtest=BEASTContextTest
```

**Example** — run a specific test class after modifying a mapping class:
```bash
mvn -pl lphybeast test -Dtest=BEASTContextTest
```

### Run

During development, `lphybeast.LPhyBeastCMD` can be launched directly via Maven using the `exec-maven-plugin`, without needing a BEAST 2 installation. Run all commands from the `lphybeast/` submodule directory (or use `-pl lphybeast` from the root).

**Show help:**
```bash
mvn -pl lphybeast compile exec:java \
  -Dexec.mainClass="lphybeast.LPhyBeastCMD" \
  -Dexec.args="-h"
```

**Convert a LPhy script to BEAST 2 XML:**
```bash
mvn -pl lphybeast exec:java \
  -Dexec.mainClass="lphybeast.LPhyBeastCMD" \
  -Dexec.args="/path/to/MyAnalysis.lphy"
```

**Specify output file name with `-o`:**
```bash
mvn -pl lphybeast exec:java \
  -Dexec.mainClass="lphybeast.LPhyBeastCMD" \
  -Dexec.args="-o MyOutput.xml /path/to/MyAnalysis.lphy"
```

**Simulate multiple XML replicates with `-r`:**
```bash
mvn -pl lphybeast exec:java \
  -Dexec.mainClass="lphybeast.LPhyBeastCMD" \
  -Dexec.args="-r 5 /path/to/hkyCoalescent.lphy"
```

**Override LPhy script variables with `-D` (macro substitution):**
```bash
mvn -pl lphybeast exec:java \
  -Dexec.mainClass="lphybeast.LPhyBeastCMD" \
  -Dexec.args="-D 'n=5;L=50' /path/to/MyAnalysis.lphy"
```

> **Note:** `exec:java` runs in the same JVM as Maven, so any JVM options (e.g. heap size, `-DLPHY_LIB`) must be passed via the `MAVEN_OPTS` environment variable rather than as arguments:
> ```bash
> MAVEN_OPTS="-Xmx4g -DLPHY_LIB=/path/to/lphy-studio/lib" \
>   mvn -pl lphybeast exec:java \
>   -Dexec.mainClass="lphybeast.LPhyBeastCMD" \
>   -Dexec.args="MyAnalysis.lphy"
> ```

The full list of available CLI options is defined in [`lphybeast/src/main/java/lphybeast/LPhyBeastCMD.java`](https://github.com/LinguaPhylo/LPhyBeast/blob/master/lphybeast/src/main/java/lphybeast/LPhyBeastCMD.java).

---

## For Developer using IntelliJ

> For a full step-by-step walkthrough, refer to the official developer guides in the LPhy repository:
> - **Guide 101 — Development Environment**: [DEV_NOTE.md](https://github.com/LinguaPhylo/linguaPhylo/blob/master/DEV_NOTE.md)
> - **Guide 102 — Maven project & IntelliJ tips**: [DEV_NOTE1.md](https://github.com/LinguaPhylo/linguaPhylo/blob/master/DEV_NOTE1.md)

### Project settings

The IntelliJ IDEA project (`.idea/`) is configured to auto-import Maven modules from the root `pom.xml`. **Do not change module or dependency settings via IntelliJ Project Structure** — any manual changes will be overwritten on the next Maven reimport.

The repo provides a curated set of IntelliJ project settings in the `IntelliJ/` directory (rather than committing `.idea/` to git). On first setup, copy these into the project root:

```bash
# From the LPhyBeast project root — check whether .idea/ already exists
ls -la | grep .idea

# If it exists and may be stale, remove it first
rm -rf .idea/

# Copy the provided settings into place
cp -r IntelliJ/.idea .idea/
```

Then open the project in IntelliJ by selecting the **root directory** (not the `pom.xml` file directly).

### Importing the project

1. If opening for the first time (or after clearing caches), go to **File → Invalidate Caches / Restart** and select all options before importing — this prevents stale index issues.
2. Click **Open** and select the project root directory. IntelliJ detects it as a Maven project automatically.
3. Wait for IntelliJ to download all dependencies and finish indexing (typically ~1 minute). A progress bar appears at the bottom while this is running.
4. Once complete, the **Maven** tool window icon should appear on the right side panel. Check for any red underlines under Maven tasks — these indicate problems.
5. Go to **Build → Rebuild Project** to compile everything. Do **not** enable "Delegate IDE build/run actions to Maven" — leave it unchecked (the default).

### Run / Debug configuration

Follow IntelliJ's [run/debug configuration tutorial](https://www.jetbrains.com/help/idea/run-debug-configuration.html) to run Java applications or unit tests directly from the IDE.

Key settings for any LPhyBEAST run configuration:

- **Main class**: `lphybeast.LPhyBeastCMD`
- **Working directory**: set to the **parent folder of the `examples/` directory** so that LPhy scripts and data files can be located correctly relative to their paths inside `.lphy` scripts
- **Program arguments**: same flags as the CLI (e.g. `RSV2.lphy`, `-r 5 hkyCoalescent.lphy`)

### Git & committing

It is recommended to use IntelliJ's built-in Git GUI for commits rather than the command line, as it makes it easier to review and stage changes file-by-file.

To improve the commit view, open the **Commit** window (left panel or **Git → Commit...**), click the **View Options** eye icon, and enable all three options under **Group By**. This groups changed files by module and directory, making large diffs much easier to navigate.

**Do not commit any IntelliJ project settings** (i.e. do not add `.idea/` to git). The shared settings live in `IntelliJ/` and are committed there intentionally.

---

## Key Interfaces for Extension Development

When writing a new LPhyBEAST extension, you must implement:

- **`lphybeast.ValueToBEAST`** — maps a LPhy `Value` object to a BEAST 2 `BEASTInterface`
- **`lphybeast.GeneratorToBEAST`** — maps a LPhy `Generator` to a BEAST 2 `BEASTInterface`
- **`lphybeast.spi.LPhyBEASTExt`** — register all your mapping classes here (the "register class")

The register class is analogous to BEAST 2's extension mechanism (uses BEAST 2's class loader, not Java's `ServiceLoader`).

Example `version.xml` for declaring BEAST 2 package dependencies:
```xml
<addon name='myextension' version='1.0.0'>
    <depends on='beast2' atleast='2.6.6'/>
    <depends on='BEASTLabs' atleast='1.9.7'/>
    <depends on='lphybeast' atleast='1.0.0'/>
</addon>
```

---

## LPhy Language Primer

LPhy scripts have two blocks:

```lphy
data {
  D = readNexus(file="alignment.nexus");
  taxa = D.taxa();
}
model {
  κ ~ LogNormal(meanlog=1.0, sdlog=0.5);
  π ~ Dirichlet(conc=[2.0, 2.0, 2.0, 2.0]);
  Q = hky(kappa=κ, freq=π);
  ψ ~ Yule(lambda=0.1, n=taxa.length());
  D ~ PhyloCTMC(L=D.nchar(), Q=Q, tree=ψ);
}
```

The `data` block clamps observed values (data clamping). The `model` block defines the probabilistic graphical model. LPhyBEAST converts this into a BEAST 2 XML.

---

## User Installation & CLI Usage

> This section covers **end-user** installation (not developer setup). For the dev environment, see [Development Environment Setup](#development-environment-setup).

### Prerequisites

- **Java 17** — LPhyBEAST requires Java 17. BEAST 2.7.x bundles Zulu 17 with JavaFX, which is the recommended runtime. Verify your version:
  ```bash
  java -version
  ```
- **BEAST 2** (latest) — [Download from beast2.org](https://www.beast2.org)
- **LPhy Studio** — Required because LPhyBEAST reads LPhy libraries from LPhy Studio's `lib/` folder

### Step 1 — Install LPhy Studio

Download and install LPhy Studio for your OS. **Use the default installation path** so that the `lphybeast` launch script can automatically detect `$LPHY_LIB`.

| OS      | Installer | Default path |
|---------|-----------|--------------|
| Mac     | `.dmg` → double-click and follow wizard | `/Applications/lphy-studio-1.7.x/` |
| Windows | `.exe` → follow wizard (use `C:\Program Files`, not `C:\Program Files (x86)`) | `C:\Program Files\lphy-studio-1.7.x\` |
| Linux   | Unzip `.zip` to home directory | `~/lphy-studio-1.7.x/` |

All releases: https://github.com/LinguaPhylo/linguaPhylo/releases

> **Windows note:** `C:\Program Files` is a protected directory. Copy the `examples/`, `tutorials/`, and `data/` folders into your `Documents` folder and work from there to avoid permission errors.

> **Linux note:** If LPhy Studio is installed in a non-default location, manually set the environment variable:
> ```bash
> export LPHY_LIB=/your/custom/path/lphy-studio-1.7.x/lib
> ```

### Step 2 — Install LPhyBEAST via BEAST 2 Package Manager

1. Launch **BEAUti** (comes with BEAST 2).
2. Go to `File` → `Manage Packages` to open the Package Manager.
3. Select **`lphybeast`** from the list and click **Install/Upgrade**.
4. Optionally, also install **`LPhyBeastExt`** if you need extended models (structured coalescent/MASCOT, mixture models, sampled ancestors, etc.).
5. Wait for the confirmation popup — installation downloads all dependent packages automatically.
6. Restart BEAUti / Package Manager to confirm packages show as "installed".

> Alternatively, install via command line — see [BEAST 2 package management docs](https://www.beast2.org/managing-packages/).

### Step 3 — Install the `lphybeast` launch script

The BEAST 2 package itself does not include the shell launcher. Download it separately and place it in your BEAST 2 `bin/` (or `bat/` on Windows) folder.

**Mac / Linux:**
```bash
# Download the script
curl -O https://raw.githubusercontent.com/LinguaPhylo/LPhyBeast/master/lphybeast/bin/lphybeast

# Make executable
chmod +x lphybeast

# Move into BEAST 2 bin
mv lphybeast /Applications/BEAST\ 2.7.x/bin/     # Mac example
# or
mv lphybeast ~/beast/bin/                          # Linux example
```

**Windows:** Download `lphybeast.bat` and move it into the `bat\` subfolder of your BEAST 2 installation.

After this step the BEAST 2 `bin/` folder should contain both the standard BEAST scripts and the new `lphybeast` script.

### Step 4 — Verify the installation

```bash
$BEAST_PATH/bin/lphybeast -h
```

Where `$BEAST_PATH` is your BEAST 2 installation directory (e.g. `/Applications/BEAST 2.7.7` on Mac). You should see the LPhyBEAST help output listing available options.

---

## Running LPhyBEAST from the Command Line

### Basic usage — convert a LPhy script to BEAST 2 XML

Navigate to the folder containing your `.lphy` script first, then run:

```bash
cd /path/to/my/analysis/
$BEAST_PATH/bin/lphybeast MyAnalysis.lphy
```

This generates `MyAnalysis.xml` in the current working directory. The working directory must also contain (or have access to) any data files referenced inside the script (e.g. `data/alignment.nex`).

**Real example** using the RSV2 tutorial:
```bash
cd $LPHY_PATH/tutorials/
$BEAST_PATH/bin/lphybeast RSV2.lphy
# Produces: RSV2.xml  (reads alignment from data/RSV2.nex)
```

### Simulate alignments with replicates (`-r`)

To produce multiple XML files each with a different simulated alignment:

```bash
cd $LPHY_PATH/examples/coalescent/
$BEAST_PATH/bin/lphybeast -r 5 hkyCoalescent.lphy
# Produces: hkyCoalescent_0.xml ... hkyCoalescent_4.xml
```

### Macro substitution (`-D`) — override LPhy script variables

Override named variables in the LPhy script from the command line without editing the file:

```bash
$BEAST_PATH/bin/lphybeast -D "n=5;L=50" MyAnalysis.lphy
```

### Common options summary

| Option | Description |
|--------|-------------|
| `-h` | Show help and all available options |
| `-r <N>` | Number of simulation replicates (generates N XML files) |
| `-D "key=val;..."` | Macro substitution: override variables in the LPhy script |
| `-o <file>` | Specify output XML filename (default: same as input with `.xml`) |
| `-l <long>` | MCMC chain length |
| `-b <int>` | Percentage of MCMC samples to discard as burnin |

Full option list: [LPhyBeastCMD.java](https://github.com/LinguaPhylo/LPhyBeast/blob/master/lphybeast/src/main/java/lphybeast/LPhyBeastCMD.java)

### Windows

Use `lphybeast.bat` in the `bat\` folder:
```bat
%BEAST_PATH%\bat\lphybeast.bat MyAnalysis.lphy
```
And work from a writable folder such as `Documents\` — not from `C:\Program Files\`.

---

## Version Compatibility

LPhyBEAST and LPhy versions must be compatible. There is **no backward compatibility** across major LPhy versions.

| LPhyBEAST version | Requires LPhy version |
|-------------------|-----------------------|
| v1.3.x | LPhy 1.7.x |
| v1.2.x | LPhy 1.6.x |
| v1.1.x | LPhy 1.5.x |
| v1.0.x | LPhy 1.4.x |

Extension compatibility (LPhyBeastExt):

| LPhyBeastExt | Requires LPhyBEAST |
|--------------|--------------------|
| 1.0.x | v1.2.x |
| 0.3.x | v1.1.x |
| 0.2.x | v1.0.x |

---

## Common Errors & Fixes

**`NoClassDefFoundError: lphy/core/LPhyParser`** — LPhy library folder not found. Check that LPhy Studio is installed in the default path, or set `$LPHY_LIB` manually.

**`IOException: Cannot find Nexus file`** — The working directory is not the parent of the `data/` subfolder. Run `lphybeast` from the directory that contains the `data/` folder, or use an absolute path in the script.

**`UnsupportedClassVersionError` (class file version 61.0)** — You are running Java 8 instead of Java 17. Install the Zulu 17 JDK bundled with BEAST 2.7.x.

**`ClassNotFoundException: beast.pkgmgmt.launcher.AppLauncherLauncher`** — The `$BEAST` environment variable is not set or points to the wrong directory. Set it to your BEAST 2 root (e.g. `export BEAST=/Applications/BEAST\ 2.7.7`).

**`Cannot find the mapping for … StructuredCoalescent`** — You are using a model that requires `LPhyBeastExt`. Install it via the BEAST 2 Package Manager.

**`Access is denied` (Windows)** — You are running from `C:\Program Files\`, a protected directory. Copy your scripts and data to `Documents\` and run from there.

**`NoClassDefFoundError: phylonco.lphy.evolution.datatype.PhasedGenotype`** — The Phylonco LPhy extension is installed in BEAST but the corresponding LPhy jar is missing from the `$LPHY_LIB` lib folder. Install the matching `phylonco` and `phylonco.lphybeast` packages, or uninstall them if you are not using Phylonco models.

---

## Dependencies

### Core LPhy libraries (Maven Central, `io.github.linguaphylo`)
- `lphy` — core language
- `lphy-base` — base distributions and functions
- `lphy-studio` — GUI (not needed for LPhyBEAST core)

### BEAST 2 packages (via `lib/` file dependencies or version.xml)
- `beast2` — BEAST 2 core
- `BEASTLabs`
- `feast`
- `BEAST_CLASSIC`
- `SMM` (substitution models)

### Known gotcha
`BEASTLabs` `beast.util.Script` depends on `jdk.nashorn.api.scripting.ScriptObjectMirror`. If you hit `NoClassDefFoundError`, add:
```
-Xbootclasspath/a:/path/to/nashorn.jar
```

---

## Development Environment Setup

1. **Java 17** is required (LTS). Check with `java -version`.
2. **Maven** for builds.
3. **IntelliJ IDEA** is the recommended IDE. Open the root `pom.xml` as an IntelliJ project.
4. The working directory for running tests or LPhy Studio should be the **parent folder of the `examples/` directory**, so LPhy scripts can be located.

### Workspace layout (recommended)
```
WorkSpace/
├── linguaPhylo/     # LPhy core repo
│   ├── lphy/
│   ├── lphy-base/
│   └── lphy-studio/
└── LPhyBeast/       # This repo
```

### IntelliJ run/debug configuration
- Set working directory to the parent of the `examples/` folder
- Use IntelliJ's GUI for committing (Git menu → Commit); enable all "Group By" view options

---

## Naming Conventions

- Java package names must **not** start with reserved names: `lphy`, `lphybeast`, `beast`
- Extension packages should follow the pattern: `myextension.lphy.evolution` (LPhy models), `myextension.lphy.spi` (SPI container class), `myextension.lphybeast` (mapping classes)

---

## Testing

See `TestInstruction.md` in the repo root. Integration tests live in the sibling repo [LPhyBeastTest](https://github.com/LinguaPhylo/LPhyBeastTest). CI runs via GitHub Actions (`.github/workflows/`).

---

## Useful Links

- [LinguaPhylo website](https://linguaphylo.github.io/)
- [Developer guide](https://linguaphylo.github.io/developer/)
- [Java extension mechanism guide](https://linguaphylo.github.io/developer/java-dev/)
- [Tutorials](https://linguaphylo.github.io/tutorials/)
- [User manual / installation](https://linguaphylo.github.io/setup/)
- [BEAST 2 Package Viewer](https://compevol.github.io/CBAN/)
- [Writing a BEAST 2 Package](https://www.beast2.org/writing-a-beast-2-package/)
- [SPI (Service Provider Interface)](https://www.baeldung.com/java-spi)
- [Java 9 Modularity guide](https://www.baeldung.com/java-9-modularity)
