# feast Class Hierarchy v11.0.0

This document maps every non-generated Java class in feast to its BEAST 3
supertype and lists the interfaces it carries. External base classes (from
`beast-base`) are shown as roots; feast-internal classes are indented below
them.

---

## Central abstraction

`LoggableRealVector` bridges the BEAST 3 `CalculationNode` / `Loggable` /
`RealVector` trio, and every computed real-vector type extends it.

The diagram below shows only the **interface relationships** — how the two
key BEAST 3 interfaces (`Loggable`, `RealVector<D>`) and the abstract base
class (`CalculationNode`) are wired together through feast's own abstract
classes, and which concrete feast classes land on each interface.

```
CalculationNode   «interface» Loggable   «interface» RealVector<D extends Real>
  [beast-base]      [beast-base]           [beast-base]
       \                  |                      /
        \_________________|_____________________/
                          |
          «abstract» LoggableRealVector<D>                    feast.realvector
          ┌──────────────┬─────────────┬──────────────────────┐
          │              │             │                       │
     ExpCalculator  Concatenate   Sequence    «abstract» CalculatedRealVector<D>



CalculationNode     «interface»       «interface»
  [beast-base]        Loggable          IntScalar<NonNegativeInt>
       \              [beast-base]        [beast-base]
        \                  |                /
         \_________________|_______________/
                           |
                   UniqueElementCount           feast.realvector
```

---

## `feast.realvector` — RealVector hierarchy

### Class hierarchy

```
CalculationNode  [beast-base]
  implements Loggable, RealVector<D extends Real>
└── LoggableRealVector<D extends Real>          [abstract]  feast.realvector
      │
      ├── ExpCalculator                          feast.expressions
      ├── Concatenate                            feast.realvector
      ├── Sequence                               feast.realvector
      │
      └── CalculatedRealVector<D extends Real>  [abstract]  feast.realvector
            │
            ├── Interleave                       feast.realvector
            ├── MetadataAsRealVector<D>          feast.realvector
            ├── Reverse                          feast.realvector
            ├── Scale                            feast.realvector
            ├── Slice                            feast.realvector
            ├── TraitSetAsRealVector             feast.realvector
            ├── TreeNodeAges                     feast.realvector   (domain: NonNegativeReal)
            ├── TreeTipAges                      feast.realvector   (domain: NonNegativeReal)
            └── ModelSelectionParameter          feast.modelselect

CalculationNode  [beast-base]
  implements Loggable, IntScalar<NonNegativeInt>
└── UniqueElementCount                           feast.realvector
```

### Class summaries

| Class | Extends | Extra interfaces |
|---|---|---|
| `LoggableRealVector<D>` | `CalculationNode` | `Loggable`, `RealVector<D>` |
| `CalculatedRealVector<D>` | `LoggableRealVector<D>` | — |
| `ExpCalculator` | `LoggableRealVector<Real>` | — |
| `Concatenate` | `LoggableRealVector<Real>` | — |
| `Sequence` | `LoggableRealVector<Real>` | — |
| `Interleave` | `CalculatedRealVector<Real>` | — |
| `MetadataAsRealVector<D>` | `CalculatedRealVector<D>` | — |
| `Reverse` | `CalculatedRealVector<Real>` | — |
| `Scale` | `CalculatedRealVector<Real>` | — |
| `Slice` | `CalculatedRealVector<Real>` | — |
| `TraitSetAsRealVector` | `CalculatedRealVector<Real>` | — |
| `TreeNodeAges` | `CalculatedRealVector<NonNegativeReal>` | — |
| `TreeTipAges` | `CalculatedRealVector<NonNegativeReal>` | — |
| `ModelSelectionParameter` | `CalculatedRealVector<Real>` | — |
| `UniqueElementCount` | `CalculationNode` | `Loggable`, `IntScalar<NonNegativeInt>` |

---

## `feast.parameter` — spec parameter subclasses

Extends the BEAST 3 spec parameter types to add file-loading and
initialisation behaviour.

```
RealVectorParam<D>  [beast-base spec]
  ├── RealVectorParamFromRealVector       feast.parameter
  ├── TimeParameter                       feast.parameter
  ├── RandomRealVectorParam               feast.parameter   implements StateNodeInitialiser
  ├── RealVectorParamFromXSV              feast.fileio
  └── RealVectorParamFromLabelledXSV      feast.fileio

IntVectorParam<D>  [beast-base spec]
  └── IntVectorParamFromIntVector         feast.parameter
```

| Class | Extends | Extra interfaces |
|---|---|---|
| `RealVectorParamFromRealVector` | `RealVectorParam<Real>` | — |
| `TimeParameter` | `RealVectorParam<Real>` | — |
| `RandomRealVectorParam` | `RealVectorParam<Real>` | `StateNodeInitialiser` |
| `RealVectorParamFromXSV` | `RealVectorParam<Real>` | — |
| `RealVectorParamFromLabelledXSV` | `RealVectorParam<Real>` | — |
| `IntVectorParamFromIntVector` | `IntVectorParam<Int>` | — |

---

## `feast.expressions` — expression calculator / distribution

```
Distribution  [beast-base]
  ├── ExpCalculatorDistribution           feast.expressions
  └── DirichletProcessPrior              feast.modelselect
```

`ExpCalculator` lives in the **RealVector hierarchy** (see above), not here —
it evaluates arithmetic expressions and exposes the result as a `RealVector<Real>`.

`ExpCalculatorDistribution` wraps an expression to produce a log-probability,
so it extends `Distribution` instead.

| Class | Extends |
|---|---|
| `ExpCalculator` | `LoggableRealVector<Real>` |
| `ExpCalculatorDistribution` | `Distribution` |

---

## `feast.operators` — operators

```
Operator  [beast-base]
  ├── BlockIntRandomWalkOperator          feast.operators
  ├── BlockIntUniformOperator             feast.operators
  ├── BlockScaleOperator                  feast.operators
  ├── DiscreteUniformJumpOperator         feast.operators
  └── SmartRealOperator  [abstract]       feast.operators
        ├── SmartRealRandomWalkOperator   feast.operators
        └── SmartScaleOperator            feast.operators
```

| Class | Extends |
|---|---|
| `BlockIntRandomWalkOperator` | `Operator` |
| `BlockIntUniformOperator` | `Operator` |
| `BlockScaleOperator` | `Operator` |
| `DiscreteUniformJumpOperator` | `Operator` |
| `SmartRealOperator` | `Operator` |
| `SmartRealRandomWalkOperator` | `SmartRealOperator` |
| `SmartScaleOperator` | `SmartRealOperator` |

---

## `feast.popmodels` — population models

```
PopulationFunction.Abstract  [beast-base]
  ├── CompoundPopulationModel             feast.popmodels
  ├── ExpressionPopulationModel           feast.popmodels
  │     └── (inner) TimeRealVectorParam   implements RealVector<Real>
  │     └── (inner) InvIntensityODE       implements FirstOrderDifferentialEquations
  │     └── (inner) InvIntensityEventHandler  implements EventHandler
  └── ShiftedPopulationModel              feast.popmodels
```

| Class | Extends |
|---|---|
| `CompoundPopulationModel` | `PopulationFunction.Abstract` |
| `ExpressionPopulationModel` | `PopulationFunction.Abstract` |
| `ShiftedPopulationModel` | `PopulationFunction.Abstract` |

---

## `feast.fileio` — file I/O

### Alignments

```
Alignment  [beast-base]
  └── AlignmentFromFile                   feast.fileio   [abstract-like base]
        ├── AlignmentFromFasta            feast.fileio
        └── AlignmentFromNexus            feast.fileio
```

### Trees

```
TreeParser  [beast-base]
  ├── TreeFromNewickFile                  feast.fileio
  └── TreeFromNexusFile                   feast.fileio
```

### Taxon sets and trait sets

```
TaxonSet  [beast-base]
  └── TaxonSetFromTree                    feast.fileio

TraitSet  [beast-base]
  ├── TipDatesFromTree                    feast.fileio
  ├── TraitSetFromTaxonSet                feast.fileio
  └── TraitSetFromXSV                     feast.fileio
```

### Log-file iteration

```
BEASTObject  [beast-base]
  ├── LogFileRealVectorParam              feast.fileio.logfileiterator
  └── LogFileState  [abstract]            feast.fileio.logfileiterator
        ├── TraceLogFileState             feast.fileio.logfileiterator
        └── TreeLogFileState              feast.fileio.logfileiterator
```

`LogFileIterator` implements `Runnable` with no BEAST supertype.

| Class | Extends | Extra interfaces |
|---|---|---|
| `AlignmentFromFile` | `Alignment` | — |
| `AlignmentFromFasta` | `AlignmentFromFile` | — |
| `AlignmentFromNexus` | `AlignmentFromFile` | — |
| `TreeFromNewickFile` | `TreeParser` | — |
| `TreeFromNexusFile` | `TreeParser` | — |
| `TaxonSetFromTree` | `TaxonSet` | — |
| `TipDatesFromTree` | `TraitSet` | — |
| `TraitSetFromTaxonSet` | `TraitSet` | — |
| `TraitSetFromXSV` | `TraitSet` | — |
| `LogFileRealVectorParam` | `BEASTObject` | — |
| `LogFileState` | `BEASTObject` | — |
| `TraceLogFileState` | `LogFileState` | — |
| `TreeLogFileState` | `LogFileState` | — |
| `LogFileIterator` | — | `Runnable` |

---

## `feast.simulation`

```
Alignment  [beast-base]
  ├── SimulatedAlignment                  feast.simulation
  └── ShuffledAlignment                   feast.simulation
```

`GPSimulator` implements `Runnable` with no BEAST supertype.

---

## `feast.nexus` — plain Java, no BEAST base

```
NexusBlock  [abstract]
  ├── CharactersBlock
  ├── TaxaBlock
  └── TreesBlock

NexusBuilder    (no supertype)
NexusWriter     (no supertype)
BasicNexusParser  (no supertype)
```

---

## `feast.modelselect`

```
CalculatedRealVector<Real>  [feast.realvector]
  └── ModelSelectionParameter

Operator  [beast-base]
  └── DirichletProcessOperator

Distribution  [beast-base]
  └── DirichletProcessPrior
```

---

## Quick-reference: all feast classes by supertype

| Supertype (beast-base) | feast classes |
|---|---|
| `CalculationNode` + `Loggable` + `RealVector<D>` | `LoggableRealVector` → `CalculatedRealVector`, `ExpCalculator`, `Concatenate`, `Sequence`, `Interleave`, `MetadataAsRealVector`, `Reverse`, `Scale`, `Slice`, `TraitSetAsRealVector`, `TreeNodeAges`, `TreeTipAges`, `ModelSelectionParameter` |
| `CalculationNode` + `IntScalar<NonNegativeInt>` | `UniqueElementCount` |
| `RealVectorParam<D>` | `RealVectorParamFromRealVector`, `TimeParameter`, `RandomRealVectorParam`, `RealVectorParamFromXSV`, `RealVectorParamFromLabelledXSV` |
| `IntVectorParam<D>` | `IntVectorParamFromIntVector` |
| `Distribution` | `ExpCalculatorDistribution`, `DirichletProcessPrior` |
| `Operator` | `BlockIntRandomWalkOperator`, `BlockIntUniformOperator`, `BlockScaleOperator`, `DiscreteUniformJumpOperator`, `SmartRealOperator` → `SmartRealRandomWalkOperator`, `SmartScaleOperator`, `DirichletProcessOperator` |
| `PopulationFunction.Abstract` | `CompoundPopulationModel`, `ExpressionPopulationModel`, `ShiftedPopulationModel` |
| `Alignment` | `AlignmentFromFile` → `AlignmentFromFasta`, `AlignmentFromNexus`; `SimulatedAlignment`, `ShuffledAlignment` |
| `TreeParser` | `TreeFromNewickFile`, `TreeFromNexusFile` |
| `TaxonSet` | `TaxonSetFromTree` |
| `TraitSet` | `TipDatesFromTree`, `TraitSetFromTaxonSet`, `TraitSetFromXSV` |
| `BEASTObject` | `LogFileRealVectorParam`, `LogFileState` → `TraceLogFileState`, `TreeLogFileState` |
| None / plain Java | `NexusBlock` tree, `NexusBuilder`, `NexusWriter`, `BasicNexusParser`, `LogFileIterator`, `GPSimulator` |
