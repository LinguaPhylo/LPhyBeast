package lphybeast;

import lphy.core.io.UserDir;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.Symbols;
import lphy.core.model.Value;
import lphy.core.parser.ObservationUtils;
import lphy.core.parser.graphicalmodel.GraphicalModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class LPhyBeastConfig {

    public static final int NUM_OF_SAMPLES = 2000;

    public final Path inPath;
    public final Path outPath;
//    public final Path wd;//TODO currently using UserDir.set/getUserDir

    /**
     * Compress the alignment only having constants sites into
     * a FilterAlignment with weights on each constant pattern.
     * If 0, as default, ignore this function;
     * If 1, then compress constants sites, where every state is compared;
     * If 2, then compress constants sites, but ignoring the unknown state or gap.
     */
    public int compressConstantAlignment = 0;

    public String[] observedParamID;

    /**
     * use LPhy simulate function instead
     */
    @Deprecated
    public boolean logAllAlignments = false;

    /**
     * has default value, and can be changed
     * chainLength   The total chain length of MCMC, default to 1 million.
     *               logEvery = chainLength / numOfSamples,
     *               where numOfSamples = 2000 as default.
     * preBurnin     The number of burnin samples taken before entering the main loop of MCMC.
     *               If < 0, as default, then it will be automatically assigned to all state nodes size * 10.
     * repId         if >= 0, then add it as postfix to the output file stem
     */
    private int preBurnin = -1; // auto estimate
    private long chainLength = 1000000; // 1M
    private long logEvery = 0;
    private int repId = -1; // >=0 for multi-outputs

    private boolean logunicode;
    // Replace the constant value in the lphy script
    private String[] lphyConst;
    // Ignoring the logging ability for the given lphy random variables
    private String[] varNotLog;
    // make sure not to initialize MCMC from the 'true' values.
    private boolean randomStart;

    // model misspecification test
    private Path model2File = null;
    public boolean log_orignal_xmls = false;

    // nested sampling
    public boolean ns = false;
    int particleCount;
    int subChainLength;
    int nsThreads;


    private boolean useMC3 = false;       // Whether to use Metropolis Coupled MCMC (MC³) instead of standard MCMC
    private int chains = 4;              // Number of parallel chains for MC³
    private double deltaTemperature = 0.15;  // Temperature increment among chains for MC³
    private int resampleEvery = 1000;    // How often chain swaps/resamples occur in MC³
    private double target = 0.234;       // Desired acceptance rate for MC³ swaps


    /**
     * The configuration to create a BEAST 2 XML.
     * Handle the input file path, output file path, and user.dir.
     * Either can be a relative or absolute path.
     * If relative, then concatenate user.dir before it.
     * @param infile   lphy script file path.
     * @param outfile  XML file path. If null,
     *                 then use the input file name stem plus .xml,
     *                 and output to the user.dir.
     * @param wd       Use to set user.dir. If null,
     *                 then set user.dir to the parent folder of lphy script.
     * @param lphyConst    constants inputted by user using macro
     */
    public LPhyBeastConfig(Path infile, Path outfile, Path wd, String[] lphyConst, String[] varNotLog,
                           boolean logunicode, boolean randomStart) throws FileNotFoundException {

        // Replace the constant value in the lphy script
        this.lphyConst = lphyConst;
        // Ignoring the logging ability for the given lphy random variables
        this.varNotLog = varNotLog;
        // whether to log IDs in unicode. If false as default,
        // the original ID in unicode will be converted to canonical letters for Windows users.
        this.logunicode = logunicode;
        // not start from the 'true' values
        this.randomStart = randomStart;

        if (infile == null || !infile.toFile().exists())
            throw new FileNotFoundException("Cannot find LPhy script file ! " + (infile != null ? infile.toAbsolutePath() : null));
        String fileName = infile.getFileName().toString();
        if (!fileName.endsWith(".lphy"))
            throw new IllegalArgumentException("Invalid LPhy file: the postfix has to be '.lphy'");

        if (wd != null)
            UserDir.setUserDir(wd.toAbsolutePath().toString());
        // this line must be between 2 conditions of wd
        // if the relative path, then concatenate user.dir before it
        this.inPath = UserDir.getUserPath(infile);

        // still need to set user.dir, if no -wd, in case LPhy script uses relative path
        if (wd == null)
            // set user.dir to the folder containing lphy script
            UserDir.setUserDir(this.inPath.getParent().toString());

        LoggerUtils.log.info("Read LPhy script from " + this.inPath.toAbsolutePath() + "\n");

        if (outfile != null) {
            this.outPath = UserDir.getUserPath(outfile);
        } else {
            String infileNoExt = getFileStem(this.inPath);
            // add wd before file stem
            this.outPath = Paths.get(UserDir.getUserDir().toString(), infileNoExt + ".xml");
        }

    }

    /**
     * The lphy script is parsed separately in string.
     * No inPath and outPath, e.g. for unit test.
     */
    public LPhyBeastConfig() throws IOException {
        inPath = null; // lphy script is in String
        outPath = null;
        preBurnin = 0;
        this.compressConstantAlignment = 0;
        this.logAllAlignments = false;
    }

    private String getFileStem(Path path) {
        String fileName = Objects.requireNonNull(path).getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf("."));
    }


    /**
     * @return   path/???.xml
     */
    public Path getXMLFilePathWithRepId() {
        if (repId < 0)
            throw new IllegalArgumentException("Invalid replicate index ! " + repId);
        final String outPathNoExt = getOutPathNoExtension(outPath);
        // update outPath to add i
        return Paths.get(getXMLFilePath(outPathNoExt));
    }

    public String getXMLFilePath(String outPathNoExt) {
        if (repId >= 0)
            return outPathNoExt + "-" + getRepId() + ".xml";
        else
            return outPathNoExt + ".xml";
    }

    public String rmParentDir(String filePath) {
        // remove any parent dir in filePath here
        if (filePath.contains(File.separator))
            filePath = filePath.substring(filePath.lastIndexOf(File.separator)+1);
        return filePath;
    }

    /**
     * @param outPath can be preprocessed, e.g. add i
     * @return outPath without the file extension,
     *         but it may not be only file name steam,
     *         it could contain the parent dir.
     */
    public String getOutPathNoExtension(Path outPath) {
        String str = Objects.requireNonNull(outPath).toString();
        return str.substring(0, str.lastIndexOf("."));
    }

    /**
     * If not using this setter, the default is 1 million chain length, and auto-detect the preBurnin,
     * the logEvery is calculated by getChainLength() / NUM_OF_SAMPLES.
     * @param chainLength  chainLength   The total chain length of MCMC.
     * @param preBurnin    The number of burnin samples taken before entering the main loop of MCMC.
     *                     If < 0, then use the default, which will be automatically assigned to all state nodes size * 10.
     * @param logEvery     The frequency to log. If < 0, then use the default,
     *                     which is calculated by getChainLength() / NUM_OF_SAMPLES.
     */
    public void setMCMCConfig(long chainLength, int preBurnin, long logEvery) {
        this.chainLength = chainLength;
        if (preBurnin > 0) this.preBurnin = preBurnin;
        if (logEvery > 0) this.logEvery = logEvery;
    }

    public int getPreBurnin() {
        return preBurnin;
    }
    public long getChainLength() {
        return chainLength;
    }

    public long getLogEvery() {
        if (logEvery > 0) return logEvery;
        // Will throw an ArithmeticException in case of overflow.
        return getChainLength() / NUM_OF_SAMPLES;
    }

    public void setMC3Config(
            boolean useMC3,
            int chains,
            double deltaTemperature,
            int resampleEvery,
            double target
    ) {
        this.useMC3 = useMC3;
        this.chains = chains;
        this.deltaTemperature = deltaTemperature;
        this.resampleEvery = resampleEvery;
        this.target = target;
    }

    /** Indicates if MC³ is enabled. */
    public boolean isUseMC3() { return useMC3; }

    /** Returns the number of chains used in MC³. */
    public int getChains() { return chains; }

    /** Returns the temperature increment (delta) for MC³ chains. */
    public double getDeltaTemperature() { return deltaTemperature; }

    /** Returns the frequency (in steps) at which MC³ resamples/swaps. */
    public int getResampleEvery() { return resampleEvery; }

    /** Returns the target acceptance rate for MC³. */
    public double getTarget() { return target; }

    public int getRepId() {
        return repId;
    }

    /**
     * @param repId    the index of replicates of simulations, >= 0.
     */
    public void setRepId(int repId) {
        if (repId >= 0) this.repId = repId;
    }

    public boolean isLogUnicode() {
        return logunicode;
    }

    public void setCompressConstantAlignment(int compressConstantAlignment) {
        this.compressConstantAlignment = compressConstantAlignment;
    }

    public String[] getLphyConst() {
        return lphyConst;
//        if (lphyConst == null) return null;
//        return Arrays.stream(lphyConst)
//                .map(String::trim)
//                // filter out empty line
//                .filter(s -> !s.isEmpty())
//                // add ; if not in the end of string
//                .map(s -> s.endsWith(";") ? s : s + ";")
//                .toArray(String[]::new);
    }

    public String[] getVarNotLog() {
        return varNotLog;
    }

    public boolean isRandomStart() {
        return randomStart;
    }

    public void setModelMisspec(Path model2File, boolean log_orignal_xmls) throws IOException {
        // same path treatment as inpth, model2File can be null
        if (model2File != null) {
            if (!model2File.toFile().exists())
                throw new FileNotFoundException("Cannot find LPhy script file ! " + model2File.toAbsolutePath());

            // If the given path is a relative path, it will return a path concatenating user.dir before the relative path.
            // Otherwise, it returns the given path.
            this.model2File = UserDir.getUserPath(model2File);
        }

        this.log_orignal_xmls = log_orignal_xmls;
    }

    public Path getModel2File() {
        return model2File;
    }

    public void setNS(boolean ns, int particleCount, int subChainLength, int nsThreads ) {
        this.ns = ns;
        this.particleCount = particleCount;
        this.subChainLength = subChainLength;
        this.nsThreads = nsThreads;
    }

    public void setObservedParamID(String[] observedParamID) {
        this.observedParamID = observedParamID;
    }

    public String[] getObservedParamID() {
        return observedParamID;
    }

    // dealing with -ob "?;?", which can specify any var in lphy to be fixed in beast2 XML.
    // return true, then fix its value, so this beast parameter/alignment will have no operator
    public boolean isObserved(Value value, GraphicalModel graphicalModel) {
        // ID can be other var, such as tree or diff parameters.
        String[] observedParamID = getObservedParamID();

        // ID can be other var, such as tree or diff parameters.
        if (observedParamID == null) {
            // as default, no -ob option, any alignments will be observed
            if (lphy.base.evolution.alignment.Alignment.class.isAssignableFrom(value.getType()))
                return true; // this keeps old lphy script working
            else return false; // not observed
        } else {
            // -ob "" can be used to trigger creating MutableAlignment
            if (observedParamID.length == 0 ||
                    (observedParamID.length == 1 && observedParamID[0].trim().isEmpty()))
                return false;
            else { // command line has -ob option
                for (String varID : observedParamID) {
                    // find ID in -ob, either symbol or canonical
                    if (varID.equals(value.getCanonicalId()) || varID.equals(Symbols.getUnicodeSymbol(value.getCanonicalId())))
                        return true;
                    // check if var is in lphy data block
                    if (ObservationUtils.isObserved(value.getId(), graphicalModel))
                        return true;
                }
            }
        }
        return false; // AlignmentToBEAST will create MutableAlignment
    }

    public int getParticleCount() {
        return particleCount;
    }

    public int getSubChainLength() {
        return subChainLength;
    }

    public int getNsThreads() {
        return nsThreads;
    }
}
