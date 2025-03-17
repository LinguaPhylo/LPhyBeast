package lphybeast;

import lphy.core.io.UserDir;
import lphy.core.logger.LoggerUtils;
import lphy.core.simulator.RandomUtils;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "lphybeast", footer = "Copyright(c) 2023",
        description = "LPhyBEAST takes an LPhy script, which contains a model specification and some data, " +
                "to produce a BEAST 2 XML file. The installation and usage is available at " +
                "https://linguaphylo.github.io/setup/",
        version = { "LPhyBEAST " + LPhyBeastCMD.VERSION,
                "Local JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                "OS: ${os.name} ${os.version} ${os.arch}"})
public class LPhyBeastCMD implements Callable<Integer> {

    public static final String VERSION = "1.2.1";

    @Parameters(paramLabel = "LPhy_scripts", description = "File of the LPhy model specification. " +
            "If it is a relative path, then concatenate 'user.dir' to the front of the path. " +
            "If `-wd` is NOT given, the 'user.dir' will set to the path where the LPhy script is.")
    Path infile;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Option(names = {"-o", "--out"},     description = "BEAST 2 XML. " +
            "If it contains relative path, then concatenate 'user.dir' to the front of the path.")
    Path outfile;
    // 'user.dir' is default to the current directory
    @Option(names = {"-wd", "--workdir"}, description = "Set 'user.dir' " +
            "and concatenate it to the front of the input and output path " +
            "(if the relative path is provided), which can be used for batch processing.")
    Path wd;

    //MCMC
    @Option(names = {"-l", "--chainLength"}, defaultValue = "1000000",
            description = "The total chain length of MCMC, default to 1 million.")
    long chainLength;
    @Option(names = {"-b", "--preBurnin"}, defaultValue = "-1",
            description = "The number of burnin samples taken before entering the main loop of MCMC. " +
                    "If < 0, as default, then estimate it based on all state nodes size.")
    int preBurnin;
    @Option(names = {"-le", "--logEvery"},
            description = "The state frequency to be logged.")
    long logEvery;

    //well calibrated study
    @Option(names = {"-r", "--replicates"}, defaultValue = "1", description = "the number of replicates (XML) given one LPhy script, " +
            "usually to create simulations for well-calibrated study.") int repTot;

    @Option(names = {"-D", "--data"}, split = ";",
            description = "Replace the constant value in the lphy script, multiple constants can be split by ';', " +
                    "but no ';' at the last: e.g. -D \"n=12;L=100\" or -D n=20")
    String[] lphyConst = null;

    @CommandLine.Option(names = {"-No", "--notlog"}, split = ";",
            description = "Ignoring the logging ability for the given lphy random variables (id or its canonical version), " +
                    "multiple id must be quoted and split by ';', but no ';' at the last: e.g. -No \"D;psi\" or -No D. " +
                    "The last means the alignment D defined in the lphy script will not be logged.")
    String[] varNotLog = null;

    @Option(names = {"-cca", "--compressConstantAlignments"},
            description = "Compress the alignment only having constants sites into " +
                    "a FilterAlignment with weights on each constant pattern.\n" +
                          "If 0, as default, ignore this function;\n" +
                          "If 1, then compress constants sites, where every state is compared;\n" +
                          "If 2, then compress constants sites, but ignoring the unknown state or gap.\n")
    int compressConstantAlignment;

    @Option(names = {"-seed"}, description = "the seed to run the LPhy script.")
    int seed;

    @Option(names = {"-u", "--logunicode"}, defaultValue = "false",
            description = "whether to log IDs in unicode. " +
                    "If false as default, the original ID in unicode will be converted to canonical letters, " +
                    "specially for avoiding the displaying issue in Windows.")
    boolean logunicode;

    @Option(names = {"-vf", "--version_file"}, split = ",",
            description = "Provide a BEAST2 version file containing a list of services to explicitly allow. " +
                    "(for package development, e.g. -vf /pkg1/version.xml,/pkg2/version.xml,/pkg3/version.xml)")
    String[] versionFiles = null;

    // model misspecification test: https://github.com/LinguaPhylo/LPhyBeast/issues/137
    @Option(names = {"-m2", "--model2"},     description = "File of the 2nd LPhy script, " +
            "which provides the model for model mis-specification test. " +
            "The simulated data (alignment, taxa dates, and species) " +
            "will be replaced by the data generated from the 1st LPhy script (given by the positional argument).")
    Path model2File = null;
    @Option(names = {"-lgX", "--logOriginalXmls"}, defaultValue = "false",
            description = "logging both original BEAST XML created by two LPhy scripts for model mis-specification.")
    boolean log_orignal_xmls;


    // MC³ (Metropolis-coupled MCMC) parameters, allowing multiple chains at different
    // "temperatures" to explore the posterior distribution more effectively:

    @Option(
            names = {"-MC3", "--mc3"},
            defaultValue = "false",
            description = "Use Metropolis Coupled MCMC (MC^3) instead of standard MCMC."
    )
    boolean useMC3;

    @Option(
            names = {"--chains"},
            defaultValue = "4",
            description = "Number of chains for MC^3 (only valid if --mc3=true)."
    )
    int chains;

    @Option(
            names = {"--deltaTemperature"},
            defaultValue = "0.15",
            description = "Delta temperature for MC^3. (only valid if --mc3=true)."
    )
    double deltaTemperature;

    @Option(
            names = {"--resampleEvery"},
            defaultValue = "1000",
            description = "Resample frequency for MC^3. (only valid if --mc3=true)."
    )
    int resampleEvery;

    @Option(
            names = {"--target"},
            defaultValue = "0.234",
            description = "Target acceptance rate for MC^3. (only valid if --mc3=true)."
    )
    double target;

    //MutableAlignment

    @Option(
            names = {"-obs"},
            description = "A Value which is observed in the model. " +
                    "By default, alignment is the observed value."
    )
    String sampleMutableAlignment;


    // TODO nested sampling
//    @Option(names = {"-NS", "--nestedSampling"}, defaultValue = "false",
//            description = "Create XML for nested sampling.")
//    boolean ns;
//    @Option(names = {"-p", "--particleCount"}, defaultValue = "100",
//            description = "The number of particles (also known as active points).")
//    int particleCount;
//    @Option(names = {"-sl", "--subChainLength"}, defaultValue = "5000",
//            description = "The length of MCMC chain used to generate a new point.")
//    int subChainLength;
//    @Option(names = {"-NSth", "--nsThreads"}, defaultValue = "1",
//            description = "The number of threads for nested sampling.")
//    int nsThreads;

//    @Option(names = {"-d", "--data"},
//            description = "Select the alignment given ID (e.g. random variable name) to compress constant sites, " +
//                    "but leave the rest unselected alignment(s) unchanged.")
//    String compressAlgId = null;

//    @Option(names = {"-lal", "--logAlignments"},
//            description = "Log all alignments including the intermediate process generated in the LPhy script.")
    @Deprecated
    boolean logAllAlignments = false;

    public static void main(String[] args) {

        // must set -Dpicocli.disable.closures=true using picocli:4.7.0
        // otherwise java.lang.NoClassDefFoundError: groovy.lang.Closure
        //	at beast.pkgmgmt.MultiParentURLClassLoader.loadClass(Unknown Source)
        // ...
        //	at lphybeast.LPhyBeastCMD.main(LPhyBeastCMD.java:88)
        int exitCode = new CommandLine(new LPhyBeastCMD()).execute(args);

        if (exitCode != 0)
            LoggerUtils.log.severe("LPhyBEAST does not exit normally !");
        System.exit(exitCode);

    }

    private static LPhyBEASTLoader loader;
    /**
     * 1. If the input/output is a relative path, then concatenate 'user.dir'
     * to the front of the path.
     * 2. Use '-wd' to set 'user.dir'. But if `-wd` is NOT given,
     * the 'user.dir' will be set to the path where the LPhy script is.
     */
    @Override
    public Integer call() throws PicocliException {
        // init loader
        if (loader == null) {
            // before LPhyBEASTLoader.getInstance()
            LPhyBEASTLoader.addBEAST2Services(versionFiles);

            loader = LPhyBEASTLoader.getInstance();
        }


        try {
            // define config for the run
            LPhyBeastConfig lPhyBeastConfig = new LPhyBeastConfig(infile, outfile, wd,
                    lphyConst, varNotLog, logunicode);
            lPhyBeastConfig.setMCMCConfig(chainLength, preBurnin, logEvery);
            // replace lphy constants
            lPhyBeastConfig.setCompressConstantAlignment(compressConstantAlignment);
            // model misspecification test
            lPhyBeastConfig.setModelMisspec(model2File, log_orignal_xmls);
            // ns
//            lPhyBeastConfig.setNS(ns, particleCount, subChainLength, nsThreads);

            lPhyBeastConfig.setMC3Config(
                    useMC3,
                    chains,
                    deltaTemperature,
                    resampleEvery,
                    target
            );

            //set whether sample alignment
            lPhyBeastConfig.setsampleMutableAlignment(sampleMutableAlignment);

            if (seed > 0)
                RandomUtils.setSeed(seed);

            LPhyBeast lphyBeast = new LPhyBeast(loader, lPhyBeastConfig, repTot);
            lphyBeast.run();

        } catch (FileNotFoundException e) {
            throw new PicocliException("Fail to read LPhy scripts from " +
                    infile + ", user.dir = " + System.getProperty(UserDir.USER_DIR), e);
        } catch (UnsupportedOperationException e) {
            throw new PicocliException("\n\nCannot find the mapping for given LPhy code to BEAST2 classes! " +
                    "\nInput file = " + infile +
                    "\nPlease ensure you have installed the required LPhyBEAST extensions and BEAST2 packages : " +
                    "\n\n" + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PicocliException(e.toString());
        }
        return 0;
    }


    /**TODO not working, use BEASTClassLoader ?
     * This function is modified from picocli demo {@code VersionProviderDemo2}.
     * {@link IVersionProvider} implementation that returns version information
     * from the lphybeast-x.x.x.jar file's {@code /META-INF/MANIFEST.MF} file.
     static class ManifestVersionProvider implements IVersionProvider {
     public String[] getVersion() throws Exception {
     Enumeration<URL> resources = LPhyBeastCMD.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
     while (resources.hasMoreElements()) {
     URL url = resources.nextElement();
     try {
     Manifest manifest = new Manifest(url.openStream());
     if (isApplicableManifest(manifest)) {
     Attributes attr = manifest.getMainAttributes();
     return new String[] { get(attr, "Implementation-Title") + " version \"" +
     get(attr, "Implementation-Version") + "\"" };
     }
     } catch (IOException ex) {
     return new String[] { "Unable to read from " + url + ": " + ex };
     }
     }
     return new String[0];
     }

     private boolean isApplicableManifest(Manifest manifest) {
     return true;
     //            Attributes attributes = manifest.getMainAttributes();
     //            return "LPhyBeastCMD".equalsIgnoreCase(get(attributes, "Implementation-Title").toString());
     }

     // no null, so .toString is safe
     private static Object get(Attributes attributes, String key) {
     Object o = attributes.get(new Attributes.Name(key));
     if (o != null) return o;
     return "";
     }
     }
     */


}
