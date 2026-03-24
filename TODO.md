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

## DONE: Value converters (25 Mar)

| Generator | beast3 type | Status |
|-----------|------------|--------|
| `Beta` | `RealScalarParam<UnitInterval>` | ✅ |
| `Normal` | `RealScalarParam<Real>` | ✅ |
| `Gamma` | `RealScalarParam<PositiveReal>` | ✅ |
| `Exp` | `RealScalarParam<NonNegativeReal>` | ✅ |
| `InverseGamma` | `RealScalarParam<PositiveReal>` | ✅ |
| `Uniform` | `RealScalarParam<Real>` | ✅ |
| `Poisson` | `IntScalarParam<NonNegativeInt>` | ✅ |
| `RandomBooleanArray` | `BoolVectorParam` | ✅ |

Generic fallback updates (25 Mar):
- ✅ `DoubleValueToBEAST` → `RealScalarParam` (with domain inference from bounds)
- ✅ `IntegerValueToBEAST` → `IntScalarParam` (with domain inference from bounds)
- ✅ `BooleanValueToBEAST` → `BoolScalarParam`
- ✅ `BooleanArrayValueToBEAST` → `BoolVectorParam`
- TODO `DoubleArrayValueToBEAST` → `RealVectorParam<Real>` (needs `createParameterWithBound` refactor)
- TODO `IntegerArrayValueToBEAST` → `IntVectorParam<Int>` (needs `createParameterWithBound` refactor)

## DONE: Distribution mappings (25 Mar)

Each returns the spec distribution directly (pattern: `DirichletToBEAST`).

| Class | Status | Notes |
|-------|--------|-------|
| `BetaToBEAST` | ✅ | `spec.distribution.Beta` |
| `NormalToBEAST` | ✅ | `spec.distribution.Normal` |
| `GammaToBEAST` | ✅ | `spec.distribution.Gamma` |
| `ExpToBEAST` | ✅ | `spec.distribution.Exponential` |
| `UniformToBEAST` | ✅ | `spec.distribution.Uniform` |
| `InverseGammaToBEAST` | ✅ | `spec.distribution.InverseGamma` |
| `PoissonToBEAST` | TODO | Needs offset handling (spec Poisson has no offset) |

Then remove `BEASTContext.createPrior()` (after Poisson done).

## DONE: Substitution models (25 Mar)

| Class | New class | Status |
|-------|-----------|--------|
| `JukesCantorToBEAST` | spec `JukesCantor` | ✅ |
| `K80ToBEAST` | spec `HKY` + equal `SimplexParam` | ✅ |
| `TN93ToBEAST` | spec `TN93` + spec `Frequencies` | ✅ |
| `F81ToBEAST` | spec `HKY` (kappa=1) + spec `Frequencies` | ✅ |
| `WAGToBEAST` | spec `WAG` + optional spec `Frequencies` | ✅ |
| `BinaryCovarionToBEAST` | spec `BinaryCovarion` | ✅ |

Then remove `BEASTContext.createBEASTFrequencies()` (after all callers migrated).

## DONE: Other generators (25 Mar)

**Migrated to spec types:**
- ✅ `YuleToBEAST` → spec `YuleModel`
- ✅ `CalibratedYuleToBeast` → spec `CalibratedYuleModel`
- ✅ `BirthDeathSampleTreeDTToBEAST` → spec `BirthDeathGernhard08Model`
- ✅ `UCLNRelaxedClockToBEAST` → spec `UCRelaxedClockModel` + spec `LogNormal`

**Already fine (no old types used directly):**
- ✅ `PopFuncCoalescentToBEAST` — passes through `context.getBEASTObject()`
- ✅ `BernoulliMultiToBEAST` — passes through `context.getBEASTObject()`

**Blocked by Prior/Function infrastructure:**
- TODO `WeightedDirichletToBEAST` — needs `createPrior` (WeightedDirichlet is ParametricDistribution, not Distribution). Fixed weights to use `getAsRealParameter`.
- TODO `RandomBooleanArrayToBEAST` — complex Poisson→Sum→Prior chain
- TODO `IIDToBEAST` — expects `Prior` from sub-generators, uses `Parameter.getDimension()`

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
