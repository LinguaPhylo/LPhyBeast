# LPhyBeast beast3 Branch — Testing Guide

**For**: Walter Xie
**Last updated**: 13 April 2026
**Branch**: `beast3`

## Prerequisites

### Already on Maven Central (no source build needed)

- **beast3 core** — `io.github.compevol:beast-base`/`beast-pkgmgmt`/`beast-fx:2.8.0-beta4`
- **BEASTLabs** — `io.github.beast2-dev:beast-labs:2.1.0-beta1`

These are pulled by Maven automatically; do not install from source unless
you're doing core development.

### Must be built from source (not yet on Maven Central)

The following SNAPSHOT dependencies are required. Build them locally in the
order below. All are now on their default branch (merges complete) except
feast and flc/bdtree (PRs still open).

```bash
# 1. LPhy (should already be installed)
cd ~/Git/linguaPhylo
mvn install -DskipTests

# 2. substmodels (master — already has beast3 Maven pom)
cd ~/Git/substmodels
mvn install -DskipTests

# 3. beast-classic (master — merged by Walter, 13 Apr)
#    Required by core lphybeast module.
cd ~/Git/beast-classic
git checkout master
mvn install -DskipTests

# 4. feast (tgvaughan/feast:beast2.8-migration — Tim's migration branch, not yet merged)
#    Required by lphybeast-feast extension module.
#    NOTE: assembly step currently fails — skip it (see open issue on tgvaughan/feast).
cd ~/Git/feast
git checkout beast2.8-migration
mvn install -DskipTests -Dassembly.skipAssembly=true

# 5. Mascot (master — merged by Nicola, 13 Apr)
#    Required by lphybeast-mascot extension module.
cd ~/Git/Mascot
git checkout master
mvn install -DskipTests

# 6. MutableAlignment (main — merged by Remco, 13 Apr)
#    Required by lphybeast-ma extension module.
cd ~/Git/MutableAlignment
git checkout main
mvn install -DskipTests

# 7. flc (alexeid/flc:beast3 — PR #10 open, awaiting Mathieu)
#    Required by lphybeast-flc extension module.
cd ~/Git/flc
git checkout beast3
mvn install -DskipTests

# 8. bdtree (fkmendes/bdtree:beast3 — PR #3 open)
#    Required by lphybeast-bdtree extension module.
cd ~/Git/bdtree
git checkout beast3
mvn install -DskipTests

# 9. LPhyBeast
cd ~/Git/LPhyBeast
git checkout beast3
mvn clean install -DskipTests
```

> **Do not** use `mvn install:install-file` to satisfy a missing dependency.
> SNAPSHOT deps must be built from source via the steps above. Historically,
> installing a pre-built jar from a different version caused classloader
> collisions like
> `class beast.base.evolution.tree.Node cannot be cast to class beast.base.evolution.tree.Node`
> and missing data types (`'nucleotide' cannot be found. Choose one of []`).
> These should now be resolved by the latest beast3 package manager fix, but
> building from source remains the recommended path.

## Run existing tests

```bash
# Core tests (H5N1, basic scripts)
mvn -pl lphybeast test

# SSM tests (skyline plots -- exercises GTR via substmodels)
mvn -pl lphybeast-ssm test

# Feast tests (RSV2 -- exercises WeightedDirichlet, kappa slicing)
mvn -pl lphybeast-feast test

# FLC tests
mvn -pl lphybeast-flc test

# Mascot tests
mvn -pl lphybeast-mascot test
```

## Test the CLI

```bash
# Show help (new subcommand structure)
mvn -pl lphybeast exec:exec -Dlphybeast.args="--help"

# Convert RSV2
mvn -pl lphybeast exec:exec -Dlphybeast.args="convert ../../linguaPhylo/tutorials/RSV2.lphy"

# Convert with replicates
mvn -pl lphybeast exec:exec -Dlphybeast.args="convert -r 3 ../../linguaPhylo/examples/coalescent/hkyCoalescent.lphy"

# Convert and run BEAST
mvn -pl lphybeast exec:exec -Dlphybeast.args="run -l 10000 ../../linguaPhylo/examples/coalescent/hkyCoalescent.lphy"

# Package management
mvn -pl lphybeast exec:exec -Dlphybeast.args="list"
```

## What changed (summary)

### Entry point

`LPhyBeastMain` replaces `LPhyBeastCMD` as the primary entry point.
Subcommands: `convert`, `run`, `install`, `list`, `remove`.

### Concatenate/Slice elimination

The old pattern of creating individual `RealParameter` state nodes and
joining them with feast's `Concatenate` is replaced by creating a single
`RealVectorParam` and using `VectorElement` (from beast3) to extract
scalar views where needed.

**Before** (RSV2 `r ~ WeightedDirichlet(...)`):
```xml
<stateNode id="r_0" spec="parameter.RealParameter">0.33</stateNode>
<stateNode id="r_1" spec="parameter.RealParameter">0.33</stateNode>
<stateNode id="r_2" spec="parameter.RealParameter">0.33</stateNode>
<x id="r" spec="feast.function.Concatenate">
    <arg idref="r_0"/><arg idref="r_1"/><arg idref="r_2"/>
</x>
```

**After**:
```xml
<stateNode id="r" spec="spec.inference.parameter.RealVectorParam"
           domain="NonNegativeReal">...</stateNode>
<mutationRate id="r_0" spec="spec.inference.parameter.VectorElement"
              vector="@r" index="0"/>
```

### Deprecated types removed

All `RealParameter`, `IntegerParameter`, `Parameter` usage removed from
`BEASTContext` (200 lines deleted). All value converters produce spec types.

### Operators

Spec `DeltaExchangeOperator` replaces deprecated `BactrianDeltaExchangeOperator`.
Tree operators unchanged (not affected by spec changes).

### Dependencies

- `lphybeast/lib/` deleted (25 JARs). All deps via Maven.
- `BEASTVector` vendored to `lphybeast.util` (removed from BEASTLabs).
- `SliceDoubleArrayToBEAST` moved from `lphybeast-feast` to core.
- feast dependency reduced to `ExpCalculator` only (expression handling).

### Depends on unmerged PRs

- CompEvol/beast3#61 — `VectorElement` class in `beast.base.spec.inference.parameter`
- BEAST2-Dev/BEASTLabs#27 — BEASTLabs spec migration + BEASTVector removal

## What to look for

1. **XML correctness**: Compare generated XML structure against previous
   output. Key differences are expected (see above), but the model
   semantics should be identical.

2. **MCMC runs**: Does `lphybeast run` produce valid BEAST runs?
   Check log files for reasonable likelihoods and ESS values.

3. **Extension modules**: Do lphybeast-ssm, lphybeast-feast, lphybeast-flc,
   lphybeast-mascot still work correctly?

4. **Package manager**: Do `install`, `list`, `remove` subcommands work?
