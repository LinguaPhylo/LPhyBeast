# LPhyBEAST

Converts LPhy probabilistic models into BEAST 2 XML input files.

## Architecture — Three-Group Bridge

This project bridges two ecosystems with incompatible module systems:

1. **LPhy** (Java 17, JPMS + SPI) — model specification language, published to Maven Central
2. **BEAST 2** (Java 1.8, no JPMS) — Bayesian phylogenetic inference, uses its own class loader
3. **LPhyBEAST** (Java 17, no JPMS — the bridge) — mapping classes that translate LPhy models to BEAST 2 objects

LPhyBEAST is packaged as BEAST 2 packages (not JPMS modules). It requires LPhy JARs on the
classpath to trigger SPI. BEAST 2 JARs are `system`-scope dependencies from `lib/` dirs
(checked into the repo, not on Maven Central).

## Modules

- `lphybeast/` — Core: `BEASTContext`, mapping interfaces, CLI entry point (`LPhyBeastCMD`)
- `lphybeast-mascot/` — Extension: MASCOT structured coalescent mappings
- `lphybeast-flc/` — Extension: Flexible Lewis coding models
- `lphybeast-mm/` — Extension: Mixture models
- `lphybeast-sa/` — Extension: Sampled ancestor models
- `lphybeast-ext-dist/` — Extension distribution packaging (bundles above extensions)

## Build & Test

The root `pom.xml` includes `../linguaPhylo` as a `<module>`, so both repos build together.
The sibling LPhy checkout is expected at `../linguaPhylo/`.
Uses Maven wrapper (`./mvnw`) for reproducible builds. Plain `mvn` also works.

```bash
# Build everything (LPhy + LPhyBeast), skip tests
./mvnw clean install -DskipTests

# Build only the core module (after dependencies are installed)
./mvnw -pl lphybeast clean install

# Build core module with its dependencies from source
./mvnw -pl lphybeast -am clean install

# Run all tests
./mvnw test

# Run tests for core module only
./mvnw -pl lphybeast test

# Run a single test class
./mvnw -pl lphybeast test -Dtest=RSV2TutorialTest
```

The project version is set by `<revision>` in the root `pom.xml` (currently `1.3.0`).
LPhy version is `<lphy.vision>1.7.0</lphy.vision>`.

## Run (Development)

No BEAST 2 installation is needed for development. The BEAST 2 JARs in `lphybeast/lib/`
(system-scope Maven deps) provide all required classes. Use `-vf version.xml` to register
mappings from the local build instead of BEAST 2's package manager.

**Via tests** — the simplest way to verify conversions:

```bash
./mvnw -pl lphybeast test -Dtest=RSV2TutorialTest
./mvnw -pl lphybeast test -Dtest=H5N1TutorialTest
```

**Via `exec:exec`** — run `LPhyBeastCMD` with full classpath including system-scope JARs:

```bash
# Show help (default)
./mvnw -pl lphybeast exec:exec

# Convert a script (paths relative to lphybeast/ working dir)
./mvnw -pl lphybeast exec:exec -Dlphybeast.args="../../linguaPhylo/tutorials/RSV2.lphy"

# With flags
./mvnw -pl lphybeast exec:exec -Dlphybeast.args="-r 5 ../../linguaPhylo/examples/coalescent/hkyCoalescent.lphy"
```

## Coding Conventions

Java 17, 4-space indentation, braces on same line.

### Adding a new mapping

1. **Value mapping** — implement `ValueToBEAST<T, S>` in `lphybeast.tobeast.values`
2. **Generator mapping** — implement `GeneratorToBEAST<T, S>` in `lphybeast.tobeast.generators`
3. **Register** — add the class to the `LPhyBEASTExt` provider (e.g., `LPhyBEASTExtImpl`):
   - `getValuesToBEASTs()` — returns list of `ValueToBEAST` classes
   - `getGeneratorToBEASTs()` — returns list of `GeneratorToBEAST` classes
   - `getDataTypeMap()` — maps `SequenceType` to BEAST 2 `DataType`
4. **Declare in `version.xml`** — register the provider class as a `<service>`:
   ```xml
   <service type="lphybeast.spi.LPhyBEASTExt">
       <provider classname="mypackage.spi.MyExtImpl"/>
   </service>
   ```

## Key Packages

- `lphybeast` — Core classes: `BEASTContext`, `ValueToBEAST`, `GeneratorToBEAST`, `LPhyBeastCMD`
- `lphybeast.tobeast.values` — Value-to-BEAST mappings (e.g., `AlignmentToBEAST`)
- `lphybeast.tobeast.generators` — Generator-to-BEAST mappings (e.g., `BetaToBEAST`)
- `lphybeast.tobeast.operators` — BEAST 2 operator strategies
- `lphybeast.spi` — `LPhyBEASTExt` interface and its core implementation `LPhyBEASTExtImpl`

## Extension Mechanism

LPhyBEAST uses **BEAST 2's class loader** (NOT Java SPI/ServiceLoader). Each BEAST 2 package
declares its dependencies and services in `version.xml`:

```xml
<package name='lphybeast' version='1.3.0'>
    <depends on='BEAST.base' atleast='2.7.8'/>
    <depends on='BEASTLabs' atleast='2.0.3'/>
    <!-- ... -->
    <service type="lphybeast.spi.LPhyBEASTExt">
        <provider classname="lphybeast.spi.LPhyBEASTExtImpl"/>
    </service>
</package>
```

Each submodule has its own `version.xml` at its root.

## Related Projects

- [LPhy (linguaPhylo)](https://github.com/LinguaPhylo/linguaPhylo) — sibling checkout at `../linguaPhylo/`
- [LPhyBeastTest](https://github.com/LinguaPhylo/LPhyBeastTest) — integration tests
