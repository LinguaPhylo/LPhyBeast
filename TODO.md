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
- `DefaultOperatorStrategy` dispatches `BoolVectorParam` → spec `BitFlipOperator`

### Infrastructure (beast3 repo)
- `BEASTClassLoader.initServicesFromClassLoaderResources()` — finds version.xml in JARs
- beast-base embeds version.xml in published JAR
- beast-classic `SVSGeneralSubstitutionModel` extends spec `ComplexSubstitutionModel`
- beast-classic `AncestralStateTreeLikelihood` extends spec `TreeLikelihood`
- beast-classic `GTRToDiscretePhylogeo` uses `BoolVectorParam`
- BEASTLabs `WeightedDirichlet` migrated to spec `TensorDistribution`

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
- ✅ `NumberArrayValueToBEAST` → `RealVectorParam` (with domain inference)
- ✅ `DoubleArray2DValueToBEAST` → `RealVectorParam<Real>`
- ✅ `DoubleArray3DValueToBEAST` → `RealVectorParam<Real>`
- ✅ `IntegerArrayValueToBEAST` → `IntVectorParam` (with domain-aware typing)

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
| `PoissonToBEAST` | ✅ | `spec.distribution.Poisson` + `OffsetInt` |
| `ExpMultiToBEAST` | ✅ | `spec.distribution.Exponential` + `IID` |

`BEASTContext.createPrior()` and `getPrior()` removed — no callers remain.

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

**Migrated (Prior removed):**
- ✅ `WeightedDirichletToBEAST` — uses spec `WeightedDirichlet` (BEASTLabs)
- ✅ `RandomBooleanArrayToBEAST` — uses spec `IntSum` + `OffsetInt(Poisson)`, no old Prior
- ✅ `CalibratedYuleToBeast` — gets spec distribution directly, uses spec `CalibrationPoint`
- ✅ `BirthDeathSampleTreeDTToBEAST` — removed old Prior fallback
- ✅ `PhyloCTMCToBEAST` — removed old Prior fallback for IID(LogNormal)

**Already migrated:**
- ✅ `IIDToBEAST` — uses spec `IID`, `ScalarDistribution`, `Vector` (no old Parameter usage)

## TODO: Infrastructure

- ✅ `PhyloCTMCToBEAST` migrated to spec `branchratemodel.Base` (old `BranchRateModel` import removed)
- Remove `BEASTContext.getAsRealParameter()` once all callers migrated
- Remove `BEASTContext.createRealParameter()` factory methods
- ✅ Removed `BEASTContext.createParameterWithBound()`
- ✅ `LeafCalibrationsToBEAST` migrated to spec `MRCAPrior` + spec distributions + `OffsetReal`
- ✅ `ValueHandler` — no old types (uses core `Function`/`StateNode` interfaces)
- `MapStringDoubleArrayValueToBEAST`, `ContinuousCharacterDataToBEAST` — blocked: uses `RealParameter.minordimension` (no spec equivalent)
- ✅ `MapValueToBEAST` migrated to `RealVectorParam<Real>` with keys
- Remaining old-type files: BEASTContext, DefaultOperatorStrategy, PhyloCTMCToBEAST

## DONE: Extension modules

- ✅ `lphybeast-sa` — all 4 generators use spec types (`RealScalarParam<PositiveReal>`, `RealScalarParam<UnitInterval>`)

## TODO: Extension modules

| Module | Scope |
|--------|-------|
| `lphybeast-bdtree` | 1 generator, complex prior handling |
| `lphybeast-feast` | 3 generators, `Function` dependency |
| `lphybeast-mm` | 1 generator (LewisMK) |
| `lphybeast-mc3` | MCMC strategy only |
| `lphybeast-ma` | 3 handlers |
