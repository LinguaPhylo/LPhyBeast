package mascot.lphybeast;

import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import mascot.dynamics.GLM;
import mascot.dynamics.RateShifts;
import mascot.glmmodel.Covariate;
import mascot.glmmodel.CovariateList;
import mascot.glmmodel.LogLinear;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a MASCOT-GLM XML written by LPhyBEAST drives the same dynamics as the
 * equivalent XML written by MASCOT's BEAUti.
 *
 * <p>Comparing the files textually is not meaningful: a BEAUti file targets an older BEAST
 * namespace ({@code beast.mascot.*}) and wraps inputs in BEAUti-only shims
 * ({@code mascot.util.Initialized*}), neither of which LPhyBEAST emits. What must agree is
 * the behaviour, so this rebuilds a {@link GLM} from each file with identical parameter
 * values and compares, for every epoch, the coalescent rate of every deme and the backwards
 * migration rate of every ordered pair.
 *
 * <p>Give it two files with
 * {@code -Dmascot.compare.a=<beauti.xml> -Dmascot.compare.b=<lphybeast.xml>}; without them
 * the test is skipped. {@link #main} runs the same comparison from the command line.
 *
 * <p>Interval <i>durations</i> are deliberately not required to match in the final epoch:
 * LPhyBEAST closes the oldest epoch with a sentinel beyond the root while BEAUti stops at the
 * oldest predictor date. MASCOT reuses the last row beyond its final shift either way, so both
 * mean "and everything older" -- which the per-epoch rate comparison confirms.
 */
public class MascotXmlEquivalenceTest {

    private static final int N_DEMES = 5;

    @Test
    public void lphyBeastMatchesBeauti() throws Exception {
        String a = System.getProperty("mascot.compare.a");
        String b = System.getProperty("mascot.compare.b");
        Assumptions.assumeTrue(a != null && b != null,
                "set -Dmascot.compare.a and -Dmascot.compare.b to two MASCOT-GLM XMLs");
        Assumptions.assumeTrue(Files.exists(Paths.get(a)) && Files.exists(Paths.get(b)),
                "both XML files must exist");

        Result r = compare(a, b);
        assertEquals(r.epochsA, r.epochsB, "epoch counts differ");
        assertTrue(r.maxCoalDiff < 1e-9,
                "coalescent rates differ by up to " + r.maxCoalDiff);
        assertTrue(r.maxMigDiff < 1e-9,
                "backwards migration rates differ by up to " + r.maxMigDiff);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: MascotXmlEquivalenceTest <beauti.xml> <lphybeast.xml>");
            System.exit(2);
        }
        Result r = compare(args[0], args[1]);
        System.out.printf("epochs              : %d vs %d%n", r.epochsA, r.epochsB);
        System.out.printf("max |coalRate diff| : %.6g%n", r.maxCoalDiff);
        System.out.printf("max |migRate  diff| : %.6g%n", r.maxMigDiff);
        System.out.println(r.maxCoalDiff < 1e-9 && r.maxMigDiff < 1e-9
                ? "==> IDENTICAL dynamics" : "==> DIFFER");
    }

    record Result(int epochsA, int epochsB, double maxCoalDiff, double maxMigDiff) {}

    static Result compare(String pathA, String pathB) throws Exception {
        GLM ga = build(pathA), gb = build(pathB);
        int n = Math.min(ga.getEpochCount(), gb.getEpochCount());
        double maxCoal = 0, maxMig = 0;
        for (int e = 0; e < n; e++) {
            double[] ca = ga.getCoalescentRate(e), cb = gb.getCoalescentRate(e);
            for (int k = 0; k < ca.length; k++) maxCoal = Math.max(maxCoal, Math.abs(ca[k] - cb[k]));
            double[] ma = ga.getBackwardsMigration(e), mb = gb.getBackwardsMigration(e);
            for (int k = 0; k < ma.length; k++) maxMig = Math.max(maxMig, Math.abs(ma[k] - mb[k]));
        }
        return new Result(ga.getIntervals().length, gb.getIntervals().length, maxCoal, maxMig);
    }

    // ------------------------------------------------------------------ XML -> GLM

    static GLM build(String path) throws Exception {
        String s = new String(Files.readAllBytes(Path.of(path))).replaceAll("(?s)<!--.*?-->", "");

        double[] rs = numbers(match(s, "(?s)<rateShifts[^>]*>(.*?)</rateShifts>"));
        List<double[]> ne = covariates(s, "NeGLM");
        List<double[]> mg = covariates(s, "migrationGLM");

        // BEAUti defers log-standardisation to MASCOT via a per-covariate transform flag;
        // apply it here so both sides are compared after the same preprocessing.
        applyTransforms(s, "NeGLM", ne);
        applyTransforms(s, "migrationGLM", mg);

        RateShifts shifts = new RateShifts();
        List<Double> vals = new ArrayList<>();
        for (double d : rs) vals.add(d);
        shifts.setInputValue("value", vals);
        shifts.initAndValidate();

        GLM glm = new GLM();
        glm.setInputValue("NeGLM", logLinear(ne, N_DEMES, rs.length, "Ne"));
        glm.setInputValue("migrationGLM", logLinear(mg, N_DEMES * (N_DEMES - 1), rs.length, "mig"));
        glm.setInputValue("rateShifts", shifts);
        glm.setInputValue("dimension", N_DEMES);
        glm.setInputValue("fromBeauti", false);
        glm.setInputValue("typeTrait", dummyTraits());
        glm.initAndValidate();
        return glm;
    }

    /** MASCOT Covariate.transform(): log, then standardise by mean and sample sd. */
    static double[] logStandardise(double[] v) {
        double[] l = new double[v.length];
        for (int i = 0; i < v.length; i++) l[i] = Math.log(v[i]);
        double mean = 0;
        for (double x : l) mean += x;
        mean /= l.length;
        double sd = 0;
        for (double x : l) sd += (x - mean) * (x - mean);
        sd = Math.sqrt(sd / (l.length - 1));
        double[] o = new double[v.length];
        for (int i = 0; i < v.length; i++) o[i] = (l[i] - mean) / sd;
        return o;
    }

    static void applyTransforms(String s, String glm, List<double[]> covs) {
        String blk = block(s, glm);
        Matcher m = Pattern.compile("<transform[^>]*>([^<]*)").matcher(blk);
        if (!m.find()) return;
        String[] flags = m.group(1).trim().split("\\s+");
        for (int k = 0; k < flags.length && k < covs.size(); k++)
            if (Boolean.parseBoolean(flags[k])) covs.set(k, logStandardise(covs.get(k)));
    }

    static String block(String s, String tag) {
        int i = s.indexOf("<" + tag), j = s.indexOf("</" + tag + ">");
        if (i < 0 || j < 0) throw new IllegalArgumentException("no <" + tag + "> in XML");
        return s.substring(i, j);
    }

    static List<double[]> covariates(String s, String glm) {
        List<double[]> out = new ArrayList<>();
        Matcher m = Pattern.compile("<covariates id=\"([^\"]*)\"[^>]*>([^<]*)").matcher(block(s, glm));
        while (m.find()) out.add(numbers(m.group(2)));
        return out;
    }

    static String match(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        if (!m.find()) throw new IllegalArgumentException("pattern not found: " + regex);
        return m.group(1);
    }

    static double[] numbers(String body) {
        String[] t = body.trim().split("\\s+");
        double[] v = new double[t.length];
        for (int i = 0; i < t.length; i++) v[i] = Double.parseDouble(t[i]);
        return v;
    }

    static LogLinear logLinear(List<double[]> covs, int verticalEntries, int nIntervals, String id)
            throws Exception {
        List<Covariate> list = new ArrayList<>();
        for (int k = 0; k < covs.size(); k++) {
            Double[] b = new Double[covs.get(k).length];
            for (int i = 0; i < b.length; i++) b[i] = covs.get(k)[i];
            list.add(new Covariate(b, id + "_" + k));
        }
        CovariateList cl = new CovariateList();
        cl.setInputValue("covariates", list);
        cl.initAndValidate();

        // identical parameter values on both sides: the files differ only in MCMC start points
        RealParameter scaler = new RealParameter();
        scaler.setInputValue("value", "1.0");
        scaler.setInputValue("dimension", covs.size());
        scaler.initAndValidate();
        BooleanParameter indicator = new BooleanParameter();
        indicator.setInputValue("value", "true");
        indicator.setInputValue("dimension", covs.size());
        indicator.initAndValidate();
        RealParameter clock = new RealParameter();
        clock.setInputValue("value", "1.0");
        clock.initAndValidate();

        LogLinear g = new LogLinear();
        g.setInputValue("covariateList", cl);
        g.setInputValue("scaler", scaler);
        g.setInputValue("indicator", indicator);
        g.setInputValue("clock", clock);
        g.setID(id);
        g.nrIntervals = nIntervals;
        g.verticalEntries = verticalEntries;
        g.initAndValidate();
        return g;
    }

    static beast.base.evolution.tree.TraitSet dummyTraits() throws Exception {
        String[] demes = {"Bombali", "Kailahun", "Kenema", "WesternRural", "WesternUrban"};
        List<beast.base.evolution.alignment.Taxon> taxa = new ArrayList<>();
        StringBuilder tr = new StringBuilder();
        for (int i = 0; i < demes.length; i++) {
            taxa.add(new beast.base.evolution.alignment.Taxon("t" + i));
            if (i > 0) tr.append(",");
            tr.append("t").append(i).append("=").append(demes[i]);
        }
        beast.base.evolution.alignment.TaxonSet ts = new beast.base.evolution.alignment.TaxonSet();
        ts.setInputValue("taxon", taxa);
        ts.initAndValidate();
        beast.base.evolution.tree.TraitSet t = new beast.base.evolution.tree.TraitSet();
        t.setInputValue("traitname", "deme");
        t.setInputValue("value", tr.toString());
        t.setInputValue("taxa", ts);
        t.initAndValidate();
        return t;
    }
}
