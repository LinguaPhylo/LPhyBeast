package lphybeast.spi;

import beast.base.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;
import lphy.base.distribution.DiscretizedGamma;
import lphy.base.distribution.RandomComposition;
import lphy.base.distribution.Sample;
import lphy.base.distribution.WeightedDirichlet;
import lphy.base.evolution.alignment.FastaAlignment;
import lphy.base.evolution.datatype.Binary;
import lphy.base.evolution.datatype.Continuous;
import lphy.base.function.*;
import lphy.base.function.alignment.*;
import lphy.base.function.datatype.BinaryDatatypeFunction;
import lphy.base.function.datatype.NucleotidesFunction;
import lphy.base.function.datatype.StandardDatatypeFunction;
import lphy.base.function.io.ReadFasta;
import lphy.base.function.io.ReadNexus;
import lphy.base.function.io.WriteFasta;
import lphy.base.function.taxa.*;
import lphy.base.function.tree.ExtantTree;
import lphy.base.function.tree.MigrationCount;
import lphy.base.function.tree.NodeCount;
import lphy.core.model.ExpressionNode;
import lphy.core.model.Generator;
import lphy.core.parser.function.ExpressionNodeWrapper;
import lphy.core.parser.function.MapFunction;
import lphy.core.parser.function.MethodCall;
import lphy.core.simulator.Simulate;
import lphy.core.vectorization.array.ArrayFunction;
import lphy.core.vectorization.operation.ElementsAt;
import lphy.core.vectorization.operation.Range;
import lphy.core.vectorization.operation.RangeList;
import lphybeast.GeneratorToBEAST;
import lphybeast.ValueToBEAST;
import lphybeast.tobeast.generators.*;
import lphybeast.tobeast.values.*;

import java.util.Map;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The "Container" provider class of SPI
 * which include a list of {@link ValueToBEAST},
 * {@link GeneratorToBEAST}, and {@link DataType}
 * to extend.
 * @author Walter Xie
 */
public class LPhyBEASTExtImpl implements LPhyBEASTExt {

    // the first matching converter is used.
    @Override
    public List<Class<? extends ValueToBEAST>> getValuesToBEASTs() {
        return Arrays.asList( DoubleArrayValueToBEAST.class,  // KeyRealParameter
                IntegerArrayValueToBEAST.class, // KeyIntegerParameter
                NumberArrayValueToBEAST.class,
                CompoundVectorToBEAST.class, // TODO handle primitive CompoundVector properly
                AlignmentToBEAST.class, // simulated alignment
                TimeTreeToBEAST.class,
                DoubleValueToBEAST.class,
                DoubleArray2DValueToBEAST.class,
                IntegerValueToBEAST.class,
                BooleanArrayValueToBEAST.class,
                BooleanValueToBEAST.class );
    }

    // the first matching converter is used.
    @Override
    public List<Class<? extends GeneratorToBEAST>> getGeneratorToBEASTs() {
        return Arrays.asList( BernoulliMultiToBEAST.class, // cannot be replaced by IID
                BetaToBEAST.class,
                BinaryCovarionToBEAST.class, // language
                BirthDeathSerialSamplingToBEAST.class,
                BirthDeathSampleTreeDTToBEAST.class,
                DirichletToBEAST.class,
                ExpToBEAST.class,
                F81ToBEAST.class,
//                FossilBirthDeathTreeToBEAST.class,
                GammaToBEAST.class,
                GTRToDiscretePhylogeo.class,
                GTRToBEAST.class,
                HKYToBEAST.class,
                IIDToBEAST.class,
                InverseGammaToBEAST.class,
                JukesCantorToBEAST.class,
                K80ToBEAST.class,
                LocalBranchRatesToBEAST.class,
                LogNormalToBEAST.class,
//                MultispeciesCoalescentToStarBEAST2.class,
                NormalToBEAST.class,
                PhyloCTMCToBEAST.class,
                PoissonToBEAST.class,
                RandomBooleanArrayToBEAST.class,
                SerialCoalescentToBEAST.class,
//                SimFBDAgeToBEAST.class,
                SkylineToBSP.class,
//                SimulateToAlignments.class,
                SliceDoubleArrayToBEAST.class,
//                StructuredCoalescentToMascot.class,
                TreeLengthToBEAST.class,
                TN93ToBEAST.class,
                UCLNRelaxedClockToBEAST.class,
                UniformToBEAST.class,
                VectorizedDistributionToBEAST.class,
                VectorizedFunctionToBEAST.class,
                WeightedDirichletToBEAST.class,
                YuleToBEAST.class, CalibratedYuleToBeast.class,
                ExpMarkovChainToBEAST.class );
    }

    // LPhy SequenceType => BEAST DataType
    @Override
    public Map<SequenceType, DataType> getDataTypeMap() {
        Map<SequenceType, DataType> dataTypeMap = new ConcurrentHashMap<>();
        dataTypeMap.put(SequenceType.NUCLEOTIDE, new beast.base.evolution.datatype.Nucleotide());
        dataTypeMap.put(SequenceType.AMINO_ACID, new beast.base.evolution.datatype.Aminoacid());
        dataTypeMap.put(Binary.getInstance(), new beast.base.evolution.datatype.Binary());
        dataTypeMap.put(Continuous.getInstance(), new beastclassic.evolution.datatype.ContinuousDataType());
        return dataTypeMap;
    }

    //*** these below are extra from Exclusion, only implemented in extensions ***//

    @Override
    public List<Class<? extends Generator>> getExcludedGenerator() {
        return List.of(
                ReadNexus.class, ReadFasta.class, WriteFasta.class, // IO
                // Taxa and sites
                NTaxaFunction.class, NCharFunction.class, TaxaFunction.class, CreateTaxa.class,
                SpeciesTaxa.class, TaxaAgesFromFunction.class, RemoveTaxa.class,
                CopySites.class, MissingSites.class, SelectSitesByMissingFraction.class, InvariableSites.class,
                VariableSites.class, //Distance.class,
                DiscretizedGamma.class,
                // Tree
                NodeCount.class, ExtractTrait.class, ExtantTree.class,
                MigrationMatrix.class, MigrationCount.class,
                // distribution
                WeightedDirichlet.class,
                // utils
                MapFunction.class, ConcatArray.class, ARange.class, Range.class, RangeList.class,
                ElementsAt.class, Rep.class, RepArray.class, Sort.class, Unique.class,
                Get.class, Length.class, Select.class, Intersect.class,
                RandomComposition.class, SumBoolean.class,
                // sequence typs
                NucleotidesFunction.class, StandardDatatypeFunction.class, BinaryDatatypeFunction.class,
                // lphy
                MethodCall.class, Simulate.class, Sample.class, ArrayFunction.class,
                ExpressionNode.class, ExpressionNodeWrapper.class
        );
    }

    @Override
    public List<Class> getExcludedValueType() {
        // For a complex logic, or arrays, use isExcludedValue
        return List.of(String.class, // ignore all String: d = nexus(file="Dengue4.nex");
                HashMap.class, TreeMap.class,
                FastaAlignment.class, // for fasta format
                SequenceType.class // ignore all data types
        );
    }


}
