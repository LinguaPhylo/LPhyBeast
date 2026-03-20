# LPhyBeast Modularisation Spec

**Date**: 20 March 2026

## Problem

The core `lphybeast` module has hard dependencies on BEAST packages that are
not yet migrated to beast3 (feast, ORC, CoupledMCMC, bdtree, MutableAlignment).
This prevents the core from compiling until all dependencies are migrated.

## Goal

Fully modularise LPhyBeast so that:
1. The core module compiles with only beast3 core + BEASTLabs + BEAST_CLASSIC
2. Each optional BEAST package dependency is isolated in its own extension module
3. Extensions are discovered at runtime via the existing `LPhyBEASTExt` SPI
4. Users only need the extensions for the BEAST packages they have installed

## Current architecture

```
lphybeast (core)
  imports: beast.base, beastlabs, feast, orc, CoupledMCMC, bdtree, MutableAlignment
  contains: 48 generators, 19 value converters, BEASTContext, operator strategies

lphybeast-sa      (extension, depends on SA)
lphybeast-mm      (extension, depends on MM)
lphybeast-mascot  (extension, depends on Mascot)
lphybeast-flc     (extension, depends on flc)
```

## Proposed architecture

```
lphybeast (core)
  imports: beast.base, beast-labs, beast-classic ONLY
  contains: core generators/values/operators with no optional deps

lphybeast-feast   (extension, depends on feast)
lphybeast-orc     (extension, depends on ORC)
lphybeast-mc3     (extension, depends on CoupledMCMC)
lphybeast-bdtree  (extension, depends on bdtree)
lphybeast-ma      (extension, depends on MutableAlignment)
lphybeast-sa      (extension, depends on SA)       [already exists]
lphybeast-mm      (extension, depends on MM)       [already exists]
lphybeast-mascot  (extension, depends on Mascot)   [already exists]
lphybeast-flc     (extension, depends on flc)      [already exists]
```

## File-by-file migration plan

### Files that move cleanly to extension modules

These files only depend on one optional package and can be moved directly.

**lphybeast-feast** (new module):
- `ExpressionNodeWrapperToFEAST.java` — feast ExpCalculator
- `ExpressionNodeToBEAST.java` — feast ExpCalculator
- `ExpressionUtils.java` — feast ExpCalculator

**lphybeast-bdtree** (new module):
- `BirthDeathSerialSamplingToBEAST.java` — bdtree BirthDeathSequentialSampling

### Files that need refactoring

These core files import multiple optional packages and need to be split.

#### BEASTContext.java

| Import | Usage | Extraction |
|--------|-------|------------|
| `CoupledMCMC` | `createMC3()` method | Extract to `MCMCStrategy` interface. Core creates standard `MCMC`. MC3 extension provides `CoupledMCMCStrategy`. |
| `Concatenate` | Checking if value is Concatenate, extracting parts | Extract to `ValueHandler` SPI. Feast extension registers a handler for Concatenate. |
| `ExpCalculator` | Checking if value is ExpCalculator | Same `ValueHandler` SPI. |

New SPI interface:
```java
public interface MCMCStrategy {
    MCMC createMCMC(long chainLength, long logEvery,
                    String logFileStem, int preBurnin);
}
```

Default: creates standard `MCMC`.
MC3 extension: creates `CoupledMCMC`.

New SPI interface:
```java
public interface ValueHandler {
    /** Can this handler process the given BEASTInterface? */
    boolean canHandle(BEASTInterface value);

    /** Extract sub-components from a compound value (e.g. Concatenate parts) */
    List<Function> extractParts(BEASTInterface value);
}
```

#### PhyloCTMCToBEAST.java

| Import | Usage | Extraction |
|--------|-------|------------|
| ORC operators (6 classes) | Added to relaxed clock analyses | Extract to `OperatorContributor` SPI. ORC extension contributes its operators. |
| `MutableAlignment` | Alternative tree likelihood | Extract to `TreeLikelihoodStrategy` SPI. MA extension provides MA-based likelihood. |

New SPI interface:
```java
public interface OperatorContributor {
    /** Return additional operators for this analysis type */
    List<Operator> getOperators(AnalysisContext context);
}
```

ORC extension implements this to add `SmallPulley`, `InConstantDistanceOperator`, etc.

#### DefaultOperatorStrategy.java

| Import | Usage | Extraction |
|--------|-------|------------|
| feast `Slice` | Operator weight for sliced parameters | Move Slice handling to feast extension's OperatorContributor |
| `MutableAlignment` | Mutable alignment operators | Move to MA extension |

#### AlignmentToBEAST.java / LPhyBeastCMD.java / LPhyBeastConfig.java

| Import | Usage | Extraction |
|--------|-------|------------|
| `MutableAlignment` | Optional MA-based alignment conversion | Guard with runtime class check, or move MA-specific logic to lphybeast-ma extension via ValueToBEAST SPI |

#### SliceDoubleArrayToBEAST.java / ExpMarkovChainToBEAST.java / DoubleArrayValueToBEAST.java

| Import | Usage | Extraction |
|--------|-------|------------|
| feast `Slice` / `Concatenate` | Slice and concatenate operations | Move to lphybeast-feast extension. Core needs alternative implementations or these converters only activate when feast is present. |

## New SPI interfaces summary

| Interface | Purpose | Default (core) | Extensions |
|-----------|---------|----------------|------------|
| `MCMCStrategy` | How to create the MCMC object | Standard MCMC | MC3 via CoupledMCMC |
| `ValueHandler` | Handle package-specific value types | None | feast: Concatenate, ExpCalculator |
| `OperatorContributor` | Add package-specific operators | None | ORC: 6 operator classes |
| `TreeLikelihoodStrategy` | How to create tree likelihood | Standard ThreadedTreeLikelihood | MA: MATreeLikelihood |

The existing `LPhyBEASTExt` SPI already handles `ValueToBEAST` and
`GeneratorToBEAST` registration. The new interfaces handle cross-cutting
concerns where optional package code is embedded in core classes.

## Extension module structure

Each new extension module follows the existing pattern:

```
lphybeast-feast/
  pom.xml              (depends on lphybeast + feast)
  src/main/java/
    feast/lphybeast/spi/
      FeastLBImpl.java  (implements LPhyBEASTExt)
    feast/lphybeast/tobeast/generators/
      ExpressionNodeWrapperToFEAST.java
      ExpressionNodeToBEAST.java
      SliceDoubleArrayToBEAST.java
      ExpMarkovChainToBEAST.java
    feast/lphybeast/tobeast/values/
      DoubleArrayValueToBEAST.java  (feast-specific parts)
    feast/lphybeast/
      FeastValueHandler.java  (implements ValueHandler)
```

## Migration order

1. **Define new SPI interfaces** in core (`MCMCStrategy`, `ValueHandler`,
   `OperatorContributor`, `TreeLikelihoodStrategy`)
2. **Refactor BEASTContext** to use `MCMCStrategy` and `ValueHandler` SPIs
3. **Refactor PhyloCTMCToBEAST** to use `OperatorContributor` SPI
4. **Create lphybeast-feast** module, move feast-dependent converters
5. **Create lphybeast-orc** module, implement `OperatorContributor`
6. **Create lphybeast-mc3** module, implement `MCMCStrategy`
7. **Create lphybeast-bdtree** module, move `BirthDeathSerialSamplingToBEAST`
8. **Create lphybeast-ma** module, move MutableAlignment-dependent code
9. **Verify core compiles with zero optional deps**

## Result

After modularisation, the core `lphybeast` module depends only on:
- `beast-base` (beast3 core)
- `beast-pkgmgmt` (package manager)
- `beast-labs` (BEASTLabs)
- `beast-classic` (BEAST Classic)
- `lphy-base` (LPhy)
- `picocli`

All optional BEAST package dependencies are in extension modules that are
discovered and loaded at runtime. The core compiles and runs independently.
