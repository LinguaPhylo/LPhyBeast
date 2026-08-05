# LPhyBeast

LPhyBEAST is a command-line program that takes an
[LPhy](http://linguaphylo.github.io/) script and produces a
[BEAST 3](https://github.com/CompEvol/beast3) XML file.

Maven coordinates: `io.github.linguaphylo:lphybeast-root` (multi-module).

## Module Structure

This project is a Maven multi-module reactor. Each module has a distinct role:

| Module | Role |
|--------|------|
| `lphybeast` | Core translator — the main released artifact. Implements the LPhy-to-BEAST XML pipeline and the `LPhyBEASTLoader` service registry. |
| `lphybeast-ssm` | Extension: substitution models (GTR, HKY, …) via the `substmodels` BEAST2 package |
| `lphybeast-feast` | Extension: feast functions via the `feast` BEAST2 package |
| `lphybeast-bdtree` | Extension: birth-death tree models via the `bdtree` BEAST2 package |
| `lphybeast-flc` | Extension: Flexible Local Clock via the `flc` BEAST2 package |
| `lphybeast-mascot` | Extension: structured coalescent via the `mascot` BEAST2 package |
| `lphybeast-mc3` | Extension: coupled MCMC via the `coupled-mcmc` BEAST2 package |
| `lphybeast-mm` | Extension: morphological models via the `morph-models` BEAST2 package |
| `lphybeast-orc` | Extension: ORC relaxed clocks via the `beast-orc` BEAST2 package |
| `lphybeast-sa` | Extension: sampled ancestors via the `sampled-ancestors` BEAST2 package |
| `lphybeast-launcher` | Development composition module — no artifact produced; assembles any combination of extension JARs on the JPMS module path for local `exec:exec` |

All external BEAST2 package versions are managed centrally in the root pom's
`<properties>` and `<dependencyManagement>`.

## Launcher Module

`lphybeast-launcher` solves a dependency composition problem specific to development.

LPhyBeast is a standalone application, not itself a BEAST2/3 package (see
[BEAST3-MIGRATION.md](BEAST3-MIGRATION.md) design principle 1), so its own extension
modules aren't "installed" anywhere — they're discovered purely by being on the JVM's
module path at launch (see [Extension mechanism](#extension-mechanism) below). During
development there is no packaged distribution, so extensions must be placed directly on
the module path via Maven's `%classpath` expansion.

Adding extension dependencies to `lphybeast` itself is not possible — every extension
already depends on `lphybeast`, which would create a circular dependency in the Maven
reactor. `lphybeast-launcher` is a separate `packaging=pom` module (it produces no JAR)
that can freely declare any combination of extensions as compile-scope dependencies
without introducing a cycle.

### Profiles

Maven profiles control which extensions are placed on the module path:

| Profile | Extension loaded |
|---------|-----------------|
| `all` _(active by default — no `-P` needed)_ | All extensions |
| `ssm` | `substmodels` — substitution models (GTR, HKY, …) |
| `feast` | `feast` — feast functions |
| `bdtree` | `bdtree` — birth-death tree models |
| `flc` | `flc` — Flexible Local Clock |
| `mascot` | `mascot` — structured coalescent |
| `mc3` | `coupled-mcmc` — coupled MCMC |
| `mm` | `morph-models` — morphological models |
| `orc` | `beast-orc` — ORC relaxed clocks |
| `sa` | `sampled-ancestors` — sampled ancestors |
| `ssm,feast,mascot,orc` (any comma-separated combination) | Selected extensions only — use `all` (no `-P`) to load everything |

Passing any explicit `-P` flag deactivates the `all` profile automatically, so only the
named profiles are active.
See [Selecting extensions](#selecting-extensions) for full command examples.

## Building from source

Requires Java 25 and Maven 3.9+. Every dependency — beast3 core, BEASTLabs, BEAST Classic,
feast, Mascot, FLC, ORC, substmodels, morph-models, sampled-ancestors, coupled-mcmc, and
LPhy — is a released version on Maven Central (see the root `pom.xml`
`<dependencyManagement>`), so no sibling repos need to be checked out or built from
source:

```bash
mvn clean package -DskipTests
```

> `lphybeast-bdtree` is excluded from the root `pom.xml`'s `<modules>` — its upstream
> `bdtree` package is still SNAPSHOT-only, so a standard build never touches it.

## Running

All commands are run from the **project root**. The `-Dlphybeast.args="..."` property
passes the subcommand and its arguments to `LPhyBeastMain`; omitting it defaults to `-h`
(top-level help). Run `mvn install -DskipTests` at least once so that all module JARs
are available to the reactor.

The examples below use `-pl lphybeast-launcher` (all extensions active by default).
Replace it with `-pl lphybeast-ssm -Pssm`, `-pl lphybeast-mascot`, etc. to run with a
specific extension module, or `-pl lphybeast` for core only. See
[Launcher Module](#launcher-module) for profile options.

### Help

```bash
# Top-level help (lists all subcommands)
mvn -pl lphybeast-launcher exec:exec

# Subcommand help
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="convert -h"
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="run -h"
```

### convert — produce BEAST XML from an LPhy script

```bash
# Basic conversion (XML written alongside the input file)
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="convert script.lphy"

# Specify output file and working directory
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="convert -o out.xml -wd path/to script.lphy"

# Set chain length and log frequency
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="convert -l 2000000 -le 2000 script.lphy"

# Override LPhy constants defined in the script
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="convert -D \"n=20;L=500\" script.lphy"

# Generate multiple replicate XMLs
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="convert -r 3 script.lphy"

# Sample from prior
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="convert -sp script.lphy"
```

Key `convert` options:

| Option | Description |
|--------|-------------|
| `-o`/`--out` | Output XML path |
| `-wd`/`--workdir` | Working directory for relative paths |
| `-l`/`--chainLength` | MCMC chain length (default: 1000000) |
| `-le`/`--logEvery` | State logging frequency |
| `-b`/`--preBurnin` | Pre-burnin samples (default: auto) |
| `-D`/`--data` | Replace script constants, semicolon-separated (e.g. `"n=20;L=500"`) |
| `-r`/`--replicates` | Number of replicate XMLs (default: 1) |
| `-sp`/`--sampleFromPrior` | Sample from prior |
| `-seed` | Random seed for the LPhy script |
| `-No`/`--notlog` | Variables to exclude from logging |
| `-ob`/`--observedParam` | Variables to mark as observed |
| `-t`/`--startingTree` | Newick file for starting tree |
| `-MC3`/`--mc3` | Enable Metropolis Coupled MCMC |

### run — convert and execute BEAST MCMC

`run` accepts all `convert` options plus the execution options below.

```bash
# Convert and run with defaults
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="run script.lphy"

# Set chain length and parallelise over 4 threads
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="run -l 2000000 --threads 4 script.lphy"

# Resume a previous run
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="run --resume script.lphy"

# Overwrite existing log files
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="run --overwrite script.lphy"
```

Key `run`-only options:

| Option | Description |
|--------|-------------|
| `--beast-seed` | BEAST MCMC random seed (default: 127) |
| `--threads` | Number of BEAST threads (default: 1) |
| `--resume` | Resume from existing state file |
| `--overwrite` | Overwrite existing log files |

### list, install, remove — package management

```bash
# List installed packages
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="list"

# List installed and available remote packages
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="list --available"

# Install a package from Maven
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="install groupId:artifactId:version"

# Remove an installed package
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="remove groupId:artifactId"
```

### Selecting extensions

Replace `-pl lphybeast-launcher` with the module that matches the extensions needed.
The `all` profile in `lphybeast-launcher` is active by default; passing any `-P` flag
deactivates it and loads only the named profiles.

```bash
# All extensions (default — no -P flag needed)
mvn -pl lphybeast-launcher exec:exec -Dlphybeast.args="convert script.lphy"

# Single extension only
mvn -pl lphybeast-launcher exec:exec -Pssm -Dlphybeast.args="convert script.lphy"

# Combination of extensions
mvn -pl lphybeast-launcher exec:exec -Pssm,mascot,mc3 -Dlphybeast.args="convert script.lphy"

# From an individual extension module (loads that extension + core)
mvn -pl lphybeast-ssm exec:exec -Dlphybeast.args="convert script.lphy"

# Core only — no extensions
mvn -pl lphybeast exec:exec -Dlphybeast.args="convert script.lphy"
```

## Running tests

```bash
mvn -pl lphybeast test            # core tests
mvn -pl lphybeast-feast test      # feast extension tests
mvn -pl lphybeast-flc test        # FLC extension tests
mvn -pl lphybeast-mascot test     # Mascot extension tests
```

## Architecture

LPhyBeast bridges two ecosystems:

1. **LPhy** (JPMS + SPI) — model specification language
2. **BEAST 3** (Java 25, JPMS) — Bayesian phylogenetic inference

The core module (`lphy.beast`) translates LPhy model objects into BEAST 3 XML via
mapping classes. Extension modules add mappings for specific BEAST packages.

### Key packages

- `lphybeast` — `BEASTContext`, `ValueToBEAST`, `GeneratorToBEAST`, `LPhyBeastMain`
- `lphybeast.tobeast.values` — value-to-BEAST mappings (e.g. `AlignmentToBEAST`)
- `lphybeast.tobeast.generators` — generator-to-BEAST mappings (e.g. `BetaToBEAST`)
- `lphybeast.tobeast.operators` — BEAST operator strategies
- `lphybeast.spi` — `LPhyBEASTMapping` interface and SPI services

### Adding a new mapping

1. **Value mapping** — implement `ValueToBEAST<T, S>` in `lphybeast.tobeast.values`
2. **Generator mapping** — implement `GeneratorToBEAST<T, S>` in `lphybeast.tobeast.generators`
3. **Register** — add the class to a `LPhyBEASTMapping` provider (e.g. `LPhyBEASTMappingImpl`):
   - `getValuesToBEASTs()` — returns list of `ValueToBEAST` classes
   - `getGeneratorToBEASTs()` — returns list of `GeneratorToBEAST` classes
   - `getDataTypeMap()` — maps `SequenceType` to BEAST `DataType`
4. **Declare in `module-info.java`** — register the provider:
   ```java
   provides lphybeast.spi.LPhyBEASTMapping with mypackage.spi.MyMappingImpl;
   ```

### Extension mechanism

LPhyBeast is a standalone application, not itself a BEAST2/3 package ([BEAST3-MIGRATION.md](BEAST3-MIGRATION.md)
design principle 1): it embeds beast3's package manager (`beast-pkgmgmt`) as a *library*
to resolve and install *other* beast3 packages for its own use, rather than being
installed itself as one.

`LPhyBEASTLoader`'s singleton constructor discovers LPhyBeast's own extensions via
`BEASTClassLoader.loadService(LPhyBEASTMapping.class)`, which primarily triggers **JPMS
module descriptor scanning**: it reads the `provides lphybeast.spi.LPhyBEASTMapping
with ...` declaration in each extension module's `module-info.java`. This only requires
the extension's jar to be on the JVM's module path — it does not depend on any package
being "installed", and Java's standard `ServiceLoader` is not used (`BEASTClassLoader`
is the authoritative loader).

> **Note on `version.xml`:** each module still ships its own `version.xml`, but for
> LPhyBeast's own extension modules that file is *not* embedded in the module's jar, so
> the JPMS scan above never reads it. It's only consumed by the explicit
> `LPhyBEASTLoader.loadServicesForTest()` / `addBEAST2Services(String[])` path — used by
> unit tests (which run on Surefire's plain classpath, where JPMS module scanning doesn't
> apply) and by IDE runs.

**Development** (Maven `exec:exec`): all dependency JARs are placed on `--module-path`
via `%classpath`; `--add-modules ALL-MODULE-PATH` adds every named JPMS module on that
path to the module graph automatically, making their `module-info.java` `provides`
declarations visible to `BEASTClassLoader.loadService()`.

**Installing other beast3 packages** (SA, Mascot, substmodels, etc. — the packages
LPhyBeast's extension modules integrate, not LPhyBeast itself): the `install`/`list`/
`remove` subcommands use beast3's `PackageManager` against LPhyBeast's *own* package
directory (`Utils6.getPackageUserDir("LPhyBEAST")`, overridable with `--packagedir`) —
not the shared `~/.beast/2.x/` BEAST2 directory. Note that installing a package this way
does not by itself make it loadable by `LPhyBEASTLoader` — its jar still needs to be on
the module path at launch (see [BEAST3-MIGRATION.md](BEAST3-MIGRATION.md) Phase 5 for
the current gap between the two).

## Testing guide

See [TESTING.md](TestInstruction.md) for detailed testing instructions.

## Tutorials

LPhy and LPhyBEAST [Tutorials](https://linguaphylo.github.io/tutorials/)
