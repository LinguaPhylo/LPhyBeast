package lphybeast;

import lphy.system.UserDir;
import lphy.util.LoggerUtils;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "lphybeast", footer = "Copyright(c) 2020",
        description = "LPhyBEAST takes an LPhy script, which contains a model specification and some data, " +
                "to produce a BEAST 2 XML file. The installation and usage is available at " +
                "https://linguaphylo.github.io/setup/",
        version = { "LPhyBEAST " + LPhyBeastCMD.VERSION,
                "Local JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                "OS: ${os.name} ${os.version} ${os.arch}"})
public class LPhyBeastCMD implements Callable<Integer> {

    public static final String VERSION = "0.4.3";

    @CommandLine.Parameters(paramLabel = "LPhy_scripts", description = "File of the LPhy model specification. " +
            "If it is a relative path, then concatenate 'user.dir' to the front of the path. " +
            "If `-wd` is NOT given, the 'user.dir' will set to the path where the LPhy script is.")
    Path infile;

    @CommandLine.Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @CommandLine.Option(names = {"-o", "--out"},     description = "BEAST 2 XML. " +
            "If it contains relative path, then concatenate 'user.dir' to the front of the path.")
    Path outfile;
    // 'user.dir' is default to the current directory
    @CommandLine.Option(names = {"-wd", "--workdir"}, description = "Set 'user.dir' " +
            "and concatenate it to the front of the input and output path " +
            "(if the relative path is provided), which can be used for batch processing.")
    Path wd;

    //MCMC
    @CommandLine.Option(names = {"-l", "--chainLength"}, defaultValue = "1000000",
            description = "The total chain length of MCMC, default to 1 million.")
    long chainLength;
    @CommandLine.Option(names = {"-b", "--preBurnin"}, defaultValue = "-1",
            description = "The number of burnin samples taken before entering the main loop of MCMC. " +
                    "If < 0, as default, then estimate it based on all state nodes size.")
    int preBurnin;

    //well calibrated study
    @CommandLine.Option(names = {"-r", "--replicates"}, defaultValue = "1", description = "the number of replicates (XML) given one LPhy script, " +
            "usually to create simulations for well-calibrated study.") int rep;


    public static void main(String[] args) {

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
    public Integer call() throws CommandLine.PicocliException {
        if (loader == null)
            loader = LPhyBEASTLoader.getInstance();

        try {
            LPhyBeast lphyBeast = new LPhyBeast(infile, outfile, wd, chainLength, preBurnin, loader);
            lphyBeast.setRep(rep);
            lphyBeast.run();
        } catch (FileNotFoundException e) {
            throw new CommandLine.PicocliException("Fail to read LPhy scripts from " +
                    infile + ", user.dir = " + System.getProperty(UserDir.USER_DIR), e);
        } catch (UnsupportedOperationException e) {
            throw new CommandLine.PicocliException("\n\nCannot find the mapping for given LPhy code to BEAST2 classes! " +
                    "\nInput file = " + infile +
                    "\nPlease ensure you have installed the required LPhyBEAST extensions and BEAST2 packages : " +
                    "\n\n" + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommandLine.PicocliException(e.toString());
        }
        return 0;
    }


    /**TODO not working, use BEASTClassLoader ?
     * This function is modified from picocli demo {@code VersionProviderDemo2}.
     * {@link CommandLine.IVersionProvider} implementation that returns version information
     * from the lphybeast-x.x.x.jar file's {@code /META-INF/MANIFEST.MF} file.
     static class ManifestVersionProvider implements CommandLine.IVersionProvider {
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
