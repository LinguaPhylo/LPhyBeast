package feast.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.OffsetReal;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import feast.expressions.ExpCalculator;
import lphy.core.model.ExpressionNode;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphy.core.parser.function.ExpressionNode2Args;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.ExpressionUtils;

import java.util.List;

public class ExpressionNodeToBEAST implements GeneratorToBEAST<ExpressionNode, BEASTInterface> {

    @Override
    public BEASTInterface generatorToBEAST(ExpressionNode expression, BEASTInterface value, BEASTContext context) {

        Value output = (Value) context.getGraphicalModelNode(value);

        if (!ExpressionUtils.isNamedModelValue(output, context)) return null;

        if (!output.getCanonicalId().equals(value.getID()))
            throw new IllegalArgumentException("The LPhy expression output ID " + output.getCanonicalId() +
                    " should match BEAST ID " + value.getID());

        BEASTInterface offsetPrior = tryOffsetReal(expression, value, context);
        if (offsetPrior != null) return offsetPrior;

        return toExpCalculator(expression, value, context);
    }

    /**
     * Detects the pattern {@code Z = Y + c}, {@code c + Y}, or {@code Y - c}, where Y is a
     * RandomVariable used only by this deterministic and Y's prior is a {@link ScalarDistribution}
     * over a real-valued scalar. Replaces Y's prior with an {@link OffsetReal} acting on Z's
     * parameter, so that Z becomes the sampled state variable.
     *
     * @return an OffsetReal wired as Z's prior, or null if the pattern doesn't match.
     */
    private BEASTInterface tryOffsetReal(ExpressionNode expression, BEASTInterface value, BEASTContext context) {
        if (!(expression instanceof ExpressionNode2Args)) return null;
        if (!(value instanceof RealScalarParam<?>)) return null;

        GraphicalModelNode[] inputs = expression.getInputValues();
        if (inputs == null || inputs.length != 2) return null;
        if (!(inputs[0] instanceof Value) || !(inputs[1] instanceof Value)) return null;

        Value left = (Value) inputs[0];
        Value right = (Value) inputs[1];

        // Identify which side is the random variable and which is a plain constant.
        Value randomVar;
        Value constantVal;
        boolean varOnLeft;
        if (isOffsetCandidate(left) && isPlainConstant(right)) {
            randomVar = left;
            constantVal = right;
            varOnLeft = true;
        } else if (isOffsetCandidate(right) && isPlainConstant(left)) {
            randomVar = right;
            constantVal = left;
            varOnLeft = false;
        } else {
            return null;
        }

        // The random variable must be used only by this deterministic, otherwise we can't
        // safely subsume it into Z's prior.
        if (randomVar.getOutputs() == null || randomVar.getOutputs().size() != 1) return null;

        if (!(left.value() instanceof Number) || !(right.value() instanceof Number)) return null;
        Object outputVal = output(expression, value, context);
        if (!(outputVal instanceof Number)) return null;

        double v0 = ((Number) left.value()).doubleValue();
        double v1 = ((Number) right.value()).doubleValue();
        double z = ((Number) outputVal).doubleValue();
        double tol = 1e-9 * Math.max(1.0, Math.abs(z));
        boolean isPlus = Math.abs(z - (v0 + v1)) <= tol;
        boolean isMinus = Math.abs(z - (v0 - v1)) <= tol;
        if (isPlus == isMinus) return null;  // ambiguous (e.g. constant=0) or neither + nor -

        double c = ((Number) constantVal.value()).doubleValue();
        double offset;
        if (isPlus) {
            offset = c;
        } else if (varOnLeft) {
            offset = -c;
        } else {
            return null;  // c - Y is not an offset; OffsetReal can't represent a sign flip
        }

        BEASTInterface yGenObj = context.getBEASTObject(randomVar.getGenerator());
        if (!(yGenObj instanceof ScalarDistribution<?, ?> innerCheck)) return null;
        if (innerCheck.isIntegerDistribution()) return null; // OffsetReal is real-valued only

        BEASTInterface yParam = context.getBEASTObject(randomVar);

        // Re-invoke Y's generator converter with value=null so the inner distribution is built
        // without a `param` input pointing at Y's now-orphaned RealScalarParam.
        GeneratorToBEAST yConverter = context.getGeneratorToBEAST(randomVar.getGenerator());
        if (yConverter == null) return null;
        BEASTInterface freshInner = yConverter.generatorToBEAST(randomVar.getGenerator(), (BEASTInterface) null, context);
        if (!(freshInner instanceof ScalarDistribution<?, ?>)) return null;
        freshInner.setID(value.getID() + ".dist");

        @SuppressWarnings("unchecked")
        ScalarDistribution<RealScalar<Real>, Double> innerTyped =
                (ScalarDistribution<RealScalar<Real>, Double>) freshInner;

        OffsetReal offsetReal = new OffsetReal();
        offsetReal.setInputValue("distribution", innerTyped);
        offsetReal.setInputValue("offset", new RealScalarParam<>(offset, Real.INSTANCE));
        offsetReal.setInputValue("param", value);
        offsetReal.setID(value.getID() + ".prior");
        offsetReal.initAndValidate();

        // Drop the now-subsumed BEAST objects for Y so they aren't picked up as separate
        // state nodes or top-level priors.
        if (yParam != null) context.removeBEASTObject(yParam);
        context.removeBEASTObject(yGenObj);

        // Z is a deterministic LPhy Value, so the standard pipeline's isState() returns false
        // and Z's parameter wasn't added to the MCMC state. Now that Z carries the offset prior
        // and is the actual sampled quantity, add it explicitly.
        Value output = (Value) context.getGraphicalModelNode(value);
        context.addStateNode((StateNode) value, output, true);

        return offsetReal;
    }

    private static Object output(ExpressionNode expression, BEASTInterface value, BEASTContext context) {
        Value out = (Value) context.getGraphicalModelNode(value);
        return out != null ? out.value() : null;
    }

    /** Y must be a sampled random variable to qualify for offset rewriting. */
    private static boolean isOffsetCandidate(Value v) {
        return v instanceof RandomVariable;
    }

    /** A "plain constant" is a Value with no upstream generator (i.e. a literal). */
    private static boolean isPlainConstant(Value v) {
        return !(v instanceof RandomVariable) && v.getGenerator() == null;
    }

    private BEASTInterface toExpCalculator(ExpressionNode expression, BEASTInterface value, BEASTContext context) {
        ExpCalculator expCalculator = new ExpCalculator();

        List<RandomVariable> args = ExpressionUtils.findArgs(expression);

        for (RandomVariable randomVariable : args) {
            BEASTInterface beastInterface = context.getBEASTObject(randomVariable);
            expCalculator.setInputValue("arg", beastInterface);
        }

        expCalculator.setInputValue("value", expression.getExpression());
        expCalculator.setInputValue("useCaching", false);
        expCalculator.setID(value.getID());
        expCalculator.initAndValidate();

        context.removeBEASTObject(value);

        return expCalculator;
    }

    @Override
    public Class<ExpressionNode> getGeneratorClass() {
        return ExpressionNode.class;
    }

    @Override
    public Class<BEASTInterface> getBEASTClass() {
        return BEASTInterface.class;
    }
}
