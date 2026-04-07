# LPhyBeast BEAST3 Migration Spec

**Author**: Alexei Drummond
**Date**: 20 March 2026
**Target**: 1 July 2026
**Branch**: `beast3`

## Overview

LPhyBeast translates LPhy probabilistic models into BEAST XML for Bayesian
phylogenetic inference. This document specifies the migration from BEAST 2.7
to BEAST 3 (2.8.0).

**Key architectural change**: LPhyBeast becomes a standalone application that
uses beast3's package manager (`beast-pkgmgmt`) to discover and load beast3
packages at runtime. It is not itself a BEAST package. This serves as a key
test case for whether an external standalone program can effectively use the
new beast package manager to depend on beast3 packages in a user environment.

## Design principles

1. **Standalone app, not a BEAST package**: LPhyBeast has its own CLI and
   distribution. No `version.xml` in CBAN, no package ZIP.

2. **Reuse beast3's package manager**: LPhyBeast uses `beast-pkgmgmt` to
   resolve beast3 packages from both CBAN (ZIP) and Maven. This validates
   that `beast-pkgmgmt` works as a library for third-party applications.

3. **LPhyBeast has its own CLI package manager**: Users can install/remove
   beast3 packages for use with LPhyBeast, backed by beast3's
   `PackageManager` and `MavenPackageResolver`.

4. **Dynamic extension discovery**: Available generator/value mappers depend
   on which beast3 packages are installed. If SA is installed, FBD generators
   are available. If Mascot is installed, structured coalescent works. The SPI
   extension system discovers what's available at runtime.

## Current architecture

```
LPhyBeast (BEAST 2 package + standalone app)
  |
  +-- lphybeast/          Core: 48 generators, 19 value converters, CLI
  +-- lphybeast-sa/       Sampled ancestors extension
  +-- lphybeast-mm/       Morphological models extension
  +-- lphybeast-mascot/   MASCOT extension
  +-- lphybeast-flc/      Flexible local clock extension
  +-- lphybeast-ext-dist/ Extension distribution packaging
  |
  Dependencies: system-scope JARs in lib/ (13 BEAST packages)
  Build: Maven, Java 17
  Entry point: LPhyBeastCMD (PicoCLI)
```

## Target architecture

```
LPhyBeast (standalone app, JPMS)
  |
  +-- lphybeast/          Core module (open module lphy.beast)
  |     +-- CLI entry point (LPhyBeastCMD, PicoCLI)
  |     +-- Package manager CLI (install/remove/list beast3 packages)
  |     +-- Generator mappers, value converters, operator strategies
  |
  +-- lphybeast-sa/       SA extension module
  +-- lphybeast-mm/       MM extension module
  +-- lphybeast-mascot/   MASCOT extension module
  +-- lphybeast-flc/      FLC extension module
  |
  Compile-time dependencies (Maven):
  +-- beast-base (io.github.compevol)
  +-- beast-pkgmgmt (io.github.compevol)
  +-- lphy-core (io.github.linguaphylo)
  +-- picocli
  |
  Runtime dependencies (via beast3 package manager):
  +-- SA, MM, feast, ORC, Mascot, BICEPS, etc.
  +-- Resolved from CBAN (ZIP) or Maven Central
```

**Build**: Maven, Java 25, JPMS modules
**Entry point**: `lphybeast.LPhyBeastCMD` (PicoCLI, unchanged)
**Extension mechanism**: SPI via `lphybeast.spi.LPhyBEASTExt`

## Package manager integration

### How it works

LPhyBeast embeds beast3's package manager as a library:

1. **At install time**: User runs `lphybeast --install SA` (or similar CLI).
   This calls beast3's `PackageManager` / `MavenPackageResolver` to download
   and install the SA package (from CBAN ZIP or Maven).

2. **At startup**: LPhyBeast uses `beast-pkgmgmt` to scan installed packages,
   creating module layers for each. The SPI system then discovers which
   `LPhyBEASTExt` implementations are available (e.g. `SALBImpl` becomes
   available when the SA package is installed).

3. **At translation time**: Only generator/value mappers whose dependencies
   are satisfied are active. If a user's LPhy script uses FBD trees but SA
   is not installed, LPhyBeast reports a clear error.

### What this tests

This is the first external application using `beast-pkgmgmt` as a library.
It validates:
- Package resolution works outside of beast3 itself
- CBAN and Maven resolution both work for third-party consumers
- Module layer creation works when the host app is not beast3
- Service discovery across package boundaries works correctly

Any issues found here feed back into `beast-pkgmgmt` improvements.

## Migration phases

### Phase 0: Build system (no code changes)

Convert from system-scope JARs to Maven dependencies.

- Delete `lib/` directory with all bundled JARs
- Java 17 -> 25
- Add beast3 Maven repository
- Add compile-time deps: `beast-base`, `beast-pkgmgmt`
- Keep LPhy deps as-is (`io.github.linguaphylo`)
- Remove all `version.xml` files
- **Verify**: `mvn compile` succeeds

### Phase 1: JPMS module declarations

Create `module-info.java` for each module.

Core module:
```java
open module lphy.beast {
    requires beast.base;
    requires beast.pkgmgmt;
    requires info.picocli;
    requires lphy.core;

    exports lphybeast;
    exports lphybeast.spi;
    exports lphybeast.tobeast;
    exports lphybeast.tobeast.values;
    exports lphybeast.tobeast.generators;
    exports lphybeast.tobeast.operators;
    exports lphybeast.tobeast.loggers;

    provides lphybeast.spi.LPhyBEASTExt with
        lphybeast.spi.LPhyBEASTExtImpl;
}
```

Extension modules require `lphy.beast` plus their beast3 package module.

**Verify**: `mvn compile` succeeds with module declarations.

### Phase 2: Resolve BEASTLabs dependencies (4 classes)

LPhyBeast imports exactly 4 classes from BEASTLabs. Each can be resolved
without waiting for the full BEASTLabs migration.

| Class | Used in | Resolution |
|-------|---------|------------|
| `BernoulliDistribution` | `BernoulliMultiToBEAST` | Replace with beast3's `Bernoulli` |
| `WeightedDirichlet` | `WeightedDirichletToBEAST` | Runtime dep from BEASTLabs (spec-migrated source at `~/Git/BEASTLabs`) |
| `Slice` | `SliceFactory`, 3 generators | ✅ Done (7 Apr). Replaced by `VectorElement` (beast3 #61) and `VectorSlice` (local). `SliceFactory` deleted. |
| `BEASTVector` | `BEASTContext`, 5 generators | Vendor (~40 lines, no external deps) |
| `RNNIMetric` | `BEASTContext` | Vendor (~150 lines, implements beast3's `TreeMetric`) |

Vendored classes go in `lphybeast.util` with a TODO to remove once
BEASTLabs is migrated or classes are absorbed into beast-base.

### Phase 3: Resolve feast dependencies (2 classes)

| Class | Used in | Resolution |
|-------|---------|------------|
| `ExpCalculator` | `ExpressionNodeWrapperToFEAST`, `FeastValueHandler` | Runtime dep via package manager. Still needed for LPhy expression handling. |
| `Concatenate` | ~~Core~~ | ✅ Done (7 Apr). Eliminated entirely. Single `RealVectorParam` + `VectorElement` replaces the Concatenate pattern. `SliceDoubleArrayToBEAST` moved to core. |

Feast dependency reduced to **expression handling only** (`ExpCalculator`).
`Concatenate` and `Slice` are no longer used anywhere in LPhyBeast.

### Phase 4: Resolve ORC dependencies (6 classes)

All ORC classes are operators used in `DefaultOperatorStrategy` and
`DefaultTreeOperatorStrategy`:

- `InConstantDistanceOperator`, `SimpleDistance`, `SmallPulley`
- `UcldScalerOperator`, `NEROperator_dAE_dBE_dCE`
- `SampleFromPriorOperator`

**Resolution**: Make ORC optional. Guard ORC operator usage behind runtime
availability checks. Fall back to beast-base operators when ORC is not
installed. ORC operators are MCMC performance optimisations, not functional
requirements.

### Phase 5: Package manager integration

Integrate beast3's `PackageManager` and `MavenPackageResolver` into
LPhyBeast's CLI.

- Add `--pkg-list`, `--pkg-install`, `--pkg-remove` subcommands
- Configure package storage (shared `~/.beast/2.8/` or own directory)
- Wire up module layer creation at startup
- Test: install SA from CBAN, verify `SALBImpl` is discovered

**Open question**: Should LPhyBeast share beast3's package directory
(`~/.beast/2.8/`) or have its own? Sharing means packages installed via
beast3 are automatically available to LPhyBeast.

### Phase 6: Audit remaining BEAST package dependencies

| Package | Used by | Action needed |
|---------|---------|---------------|
| BEAST_CLASSIC | core | Audit imports, determine if essential |
| SSM | core subst models | Check if absorbed into beast-base |
| CoupledMCMC | core | Audit: likely optional (parallel tempering) |
| bdtree | core | Audit: `BirthDeathSequentialSampling` |
| MutableAlignment | core | Audit: `MATreeLikelihood` etc. |
| BICEPS | core operators | Remco migrating; make optional |
| Mascot | lphybeast-mascot | Nicola migrating; extension only |
| flc | lphybeast-flc | ✅ Done (6 Apr). `io.github.compevol:flc:1.3.0-SNAPSHOT`, full spec types, test passing. |

For each: determine number of classes imported, whether usage can be made
optional, and whether the owning developer is migrating it.

### Phase 7: Update generator mappers and value converters

The 48 generator mappers and 19 value converters construct BEAST objects.
Most will work with minimal changes since beast3 maintains the
`Input`/`BEASTObject` system alongside the new spec types.

Changes needed:
- Package renames for any classes that moved in beast3
- Replace deprecated BEAST2 API calls
- Adapt to any changed constructors or method signatures

### Phase 8: Tests and CI

- Update CI workflow: Java 17 -> 25
- Configure GitHub Packages authentication for beast3 deps
- Verify all existing tests pass against beast3
- Add integration test: LPhy script -> BEAST3 XML -> BEAST3 run
- Test package manager: install packages, verify SPI discovery

## Work allocation

Alexei does the core migration work (Phases 0-5) in the two weeks before
Haoyuan starts. Haoyuan then focuses on LPhy2 and PhyloSpec integration
rather than beast3 plumbing.

| Task | Owner | Blocked by | Target |
|------|-------|------------|--------|
| Phase 0: Build system | Alexei | Nothing | w/c 24 Mar |
| Phase 1: JPMS modules | Alexei | Phase 0 | w/c 24 Mar |
| Phase 2: BEASTLabs deps | Alexei | Phase 1 | w/c 31 Mar |
| Phase 3: feast deps | Alexei | Tim (feast) | w/c 31 Mar |
| Phase 4: ORC deps | Alexei | Phase 2 | w/c 31 Mar |
| Phase 5: Package manager | Alexei | Phase 1 | w/c 31 Mar |
| Phase 6: Other pkg deps | Alexei + Haoyuan | Phase 1 | April |
| Phase 7: Mappers/converters | Haoyuan | Phases 2-6 | April-May |
| Phase 8: Tests and CI | Haoyuan | Phase 7 | May-June |

## Open questions

1. **LPhy itself**: Does LPhy (linguaPhylo) need changes for Java 25 / JPMS?
   Currently Java 17. LPhyBeast builds alongside it as a sibling module.

2. **Shared vs separate package directory**: Should LPhyBeast use
   `~/.beast/2.8/` (shared with beast3) or its own directory for installed
   packages?

3. **feast timeline**: When will Tim release feast for beast3? Blocks Phase 3.

4. **ORC timeline**: When will Jordan release ORC for beast3? LPhyBeast can
   proceed without it (optional).

5. **Distribution packaging**: How will LPhyBeast be distributed to end users?
   Fat JAR? Platform installer? Maven artifact?

6. **Backward compatibility**: Is this a clean break from BEAST2, or should
   LPhyBeast retain the ability to generate BEAST2 XML?

7. **beast-pkgmgmt API surface**: Is `beast-pkgmgmt` designed to be used as
   a library by external apps? Are there assumptions (hardcoded paths,
   beast3-specific bootstrapping) that need to be generalised?
