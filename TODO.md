# Beast3 Strong Typing Migration

## Architecture

Producer-generator-keyed `ValueToBEAST` converters create the correct beast3
parameter type (e.g. `SimplexParam`, `RealScalarParam<PositiveReal>`).
Generator mappings return spec distributions directly — no `Prior` wrapper.
Spec distributions ARE `Distribution` objects that go straight into the posterior.
See `DirichletValueToBEAST` + `DirichletToBEAST` + `GTRToBEAST` as the template.

Plan file: `~/.claude/plans/breezy-scribbling-sutherland.md`

## Done

### Value converters
- `DirichletValueToBEAST` → `SimplexParam`
- `LogNormalValueToBEAST` → `RealScalarParam<PositiveReal>`
- `ExpMarkovChainValueToBEAST` → `RealVectorParam<PositiveReal>`
- `RandomCompositionValueToBEAST` → `IntSimplexParam<PositiveInt>`

### Distribution mappings (spec, no Prior)
- `DirichletToBEAST` → `beast.base.spec.inference.distribution.Dirichlet`
- `LogNormalToBEAST` → `beast.base.spec.inference.distribution.LogNormal`
- `ExpMarkovChainToBEAST` → `beast.base.spec.inference.distribution.MarkovChainDistribution`

### Substitution models
- `GTRToBEAST` → `substmodels.nucleotide.GTR` + spec `Frequencies`
- `HKYToBEAST` → spec `HKY` + spec `Frequencies`

### Tree/site models
- `SerialCoalescentToBEAST` → spec `ConstantPopulation`
- `SkylineToBSP` → spec `BayesianSkyline`
- `PhyloCTMCToBEAST` → spec `SiteModel`
- `GTRToDiscretePhylogeo` → spec `ComplexSubstitutionModel` (beast-classic migrated)

### Operators
- `DefaultOperatorStrategy` dispatches `SimplexParam`, `IntSimplexParam`,
  `RealVectorParam`, `RealScalarParam` → spec `ScaleOperator`, `DeltaExchangeOperator`

### Infrastructure (beast3 repo)
- `BEASTClassLoader.initServicesFromClassLoaderResources()` — finds version.xml in JARs
- beast-base embeds version.xml in published JAR
- beast-classic `SVSGeneralSubstitutionModel` extends spec `ComplexSubstitutionModel`

### Tests passing
- `SkylinePlotsTutorialTest`
- `H5N1TutorialTest`

---

## TODO: Value converters needed

Each LPhy distribution needs a `ValueToBEAST` registered before the generic fallback.

| Generator | beast3 type | Notes |
|-----------|------------|-------|
| `Beta` | `RealScalarParam<UnitInterval>` | |
| `Normal` | `RealScalarParam<Real>` | |
| `Gamma` | `RealScalarParam<PositiveReal>` | |
| `Exp` | `RealScalarParam<NonNegativeReal>` | |
| `InverseGamma` | `RealScalarParam<PositiveReal>` | |
| `Uniform` | `RealScalarParam<Real>` | |
| `Poisson` | `IntScalarParam<NonNegativeInt>` | |
| `RandomBooleanArray` | `BoolVectorParam` | |

Then update generic fallbacks:
- `DoubleArrayValueToBEAST` → `RealVectorParam<Real>`
- `DoubleValueToBEAST` → `RealScalarParam<Real>`
- `IntegerArrayValueToBEAST` → `IntVectorParam<Int>`
- `IntegerValueToBEAST` → `IntScalarParam<Int>`
- `BooleanValueToBEAST` → `BoolScalarParam`
- `BooleanArrayValueToBEAST` → `BoolVectorParam`

## TODO: Distribution mappings (eliminate Prior + Function)

Each returns the spec distribution directly (pattern: `DirichletToBEAST`).

| Class | Old type | New spec type |
|-------|----------|--------------|
| `BetaToBEAST` | `Prior` | `beast...spec...distribution.Beta` |
| `NormalToBEAST` | `Prior` | `beast...spec...distribution.Normal` |
| `GammaToBEAST` | `Prior` | `beast...spec...distribution.Gamma` |
| `ExpToBEAST` | `Prior` | `beast...spec...distribution.Exponential` |
| `UniformToBEAST` | `Prior` | `beast...spec...distribution.Uniform` |
| `InverseGammaToBEAST` | `Prior` | `beast...spec...distribution.InverseGamma` |
| `PoissonToBEAST` | `Prior` | `beast...spec...distribution.Poisson` |

Then remove `BEASTContext.createPrior()`.

## TODO: Substitution models

| Class | New class |
|-------|-----------|
| `JukesCantorToBEAST` | spec `JukesCantor` or `substmodels.nucleotide.JC` |
| `K80ToBEAST` | spec or `substmodels.nucleotide.K80` |
| `TN93ToBEAST` | spec `TN93` or `substmodels.nucleotide.TrN` |
| `F81ToBEAST` | spec or `substmodels.nucleotide.F81` |
| `WAGToBEAST` | spec `WAG` |
| `BinaryCovarionToBEAST` | spec `BinaryCovarion` |

Then remove `BEASTContext.createBEASTFrequencies()`.

## TODO: Other generators

- `YuleToBEAST`, `CalibratedYuleToBeast`, `BirthDeathSampleTreeDTToBEAST`
- `PopFuncCoalescentToBEAST`, `UCLNRelaxedClockToBEAST`
- `IIDToBEAST` → spec `IID`
- `VectorizedDistributionToBEAST`, `VectorizedFunctionToBEAST`
- `BernoulliMultiToBEAST`, `WeightedDirichletToBEAST`, `RandomBooleanArrayToBEAST`

## TODO: Infrastructure

- `BranchRateModel.clock.rate` expects `Function` — needs beast3 fix
  (currently bridged in `PhyloCTMCToBEAST.getClockRateParam`)
- Remove `BEASTContext.getAsRealParameter()` once all callers migrated
- Remove `BEASTContext.createRealParameter()` factory methods

## TODO: Extension modules

| Module | Scope |
|--------|-------|
| `lphybeast-sa` | 4 generators, SA operator strategy |
| `lphybeast-bdtree` | 1 generator, complex prior handling |
| `lphybeast-feast` | 3 generators, `Function` dependency |
| `lphybeast-mm` | 1 generator (LewisMK) |
| `lphybeast-mc3` | MCMC strategy only |
| `lphybeast-ma` | 3 handlers |
