# LPhyBeast

LPhyBEAST is a command-line program that takes an
[LPhy](http://linguaphylo.github.io/) script and produces a
[BEAST 3](https://github.com/CompEvol/beast3) XML file.

Maven coordinates: `io.github.linguaphylo:lphybeast-root` (multi-module).

## Building from source

Requires Java 25 and Maven.

Build prerequisites first (each on its `beast3` or `vector-element` branch):

```bash
# 1. beast3
cd ~/Git/beast3 && git checkout vector-element && mvn install -DskipTests

# 2. BEASTLabs
cd ~/Git/BEASTLabs && git checkout beast3 && mvn install -DskipTests

# 3. LPhy
cd ~/Git/linguaPhylo && mvn install -DskipTests
```

Then build LPhyBeast:

```bash
cd ~/Git/LPhyBeast
mvn clean install -DskipTests
```

## Running

```bash
# Show help
mvn -pl lphybeast exec:exec -Dlphybeast.args="--help"

# Convert an LPhy script to BEAST XML
mvn -pl lphybeast exec:exec -Dlphybeast.args="convert ../../linguaPhylo/tutorials/RSV2.lphy"

# Convert and run BEAST
mvn -pl lphybeast exec:exec -Dlphybeast.args="run -l 10000 ../../linguaPhylo/tutorials/hkyCoalescent.lphy"

# Package management
mvn -pl lphybeast exec:exec -Dlphybeast.args="list"
```

## Running tests

```bash
mvn -pl lphybeast test            # core tests
mvn -pl lphybeast-feast test      # feast extension tests
mvn -pl lphybeast-flc test        # FLC extension tests
mvn -pl lphybeast-mascot test     # Mascot extension tests
```

## Modules

| Module | Description |
|--------|-------------|
| `lphybeast` | Core translator |
| `lphybeast-bdtree` | Birth-death tree extension |
| `lphybeast-feast` | feast extension |
| `lphybeast-flc` | Flexible Local Clock extension |
| `lphybeast-ma` | MutableAlignment extension |
| `lphybeast-mascot` | Mascot extension |
| `lphybeast-mc3` | CoupledMCMC extension |
| `lphybeast-mm` | Morphological models extension |
| `lphybeast-orc` | ORC extension |
| `lphybeast-sa` | Sampled ancestors extension |

## Architecture

LPhyBeast bridges two ecosystems:

1. **LPhy** (Java 17, JPMS + SPI) -- model specification language
2. **BEAST 3** (Java 25, JPMS) -- Bayesian phylogenetic inference

The core module (`lphy.beast`) translates LPhy model objects into BEAST 3 XML
via mapping classes. Extension modules add mappings for specific BEAST packages.

### Key packages

- `lphybeast` -- `BEASTContext`, `ValueToBEAST`, `GeneratorToBEAST`, `LPhyBeastMain`
- `lphybeast.tobeast.values` -- value-to-BEAST mappings (e.g. `AlignmentToBEAST`)
- `lphybeast.tobeast.generators` -- generator-to-BEAST mappings (e.g. `BetaToBEAST`)
- `lphybeast.tobeast.operators` -- BEAST operator strategies
- `lphybeast.spi` -- `LPhyBEASTMapping` interface and SPI services

### Adding a new mapping

1. **Value mapping** -- implement `ValueToBEAST<T, S>` in `lphybeast.tobeast.values`
2. **Generator mapping** -- implement `GeneratorToBEAST<T, S>` in `lphybeast.tobeast.generators`
3. **Register** -- add the class to a `LPhyBEASTMapping` provider (e.g. `LPhyBEASTMappingImpl`):
   - `getValuesToBEASTs()` -- returns list of `ValueToBEAST` classes
   - `getGeneratorToBEASTs()` -- returns list of `GeneratorToBEAST` classes
   - `getDataTypeMap()` -- maps `SequenceType` to BEAST `DataType`
4. **Declare in `module-info.java`** -- register the provider:
   ```java
   provides lphybeast.spi.LPhyBEASTMapping with mypackage.spi.MyMappingImpl;
   ```

### Extension mechanism

LPhyBeast uses Java SPI (`ServiceLoader`) via JPMS `module-info.java` declarations.
The core module declares `uses lphybeast.spi.LPhyBEASTMapping` and each extension
module provides its own implementation.

## Testing guide

See [TESTING.md](TESTING.md) for detailed testing instructions.

## Tutorials

LPhy and LPhyBEAST [Tutorials](https://linguaphylo.github.io/tutorials/)
