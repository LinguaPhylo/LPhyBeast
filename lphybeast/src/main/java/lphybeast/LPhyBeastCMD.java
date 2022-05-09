package lphybeast;

import lphy.util.LoggerUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "lphybeast", footer = "Copyright(c) 2020",
        description = "LPhyBEAST takes an LPhy model specification and some data, " +
                "and produces a BEAST 2 XML file. The installation and usage is available at " +
                "https://linguaphylo.github.io/setup/",
        version = { "LPhyBEAST " + LPhyBeastCMD.VERSION,
                "Local JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                "OS: ${os.name} ${os.version} ${os.arch}"})
public class LPhyBeastCMD implements Callable<Integer> {

    public static final String VERSION = "1.0.0";

    @CommandLine.Parameters(paramLabel = "LPhy_scripts", description = "File of the LPhy model specification. " +
            "If it is a relative path, then concatenate 'user.dir' to the front of the path. " +
            "But if `-wd` is NOT given, the 'user.dir' will set to the path where the LPhy script is.")
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
    @CommandLine.Option(names = {"-l", "--chainLength"}, defaultValue = "-1", description = "define the total chain length of MCMC, default to 1 million.")
    int chainLength;
    @CommandLine.Option(names = {"-b", "--preBurnin"}, defaultValue = "0", description = "define the number of burn in samples taken before entering the main loop of MCMC")
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


    /**
     * 1. If the input/output is a relative path, then concatenate 'user.dir'
     * to the front of the path.
     * 2. Use '-wd' to set 'user.dir'. But if `-wd` is NOT given,
     * the 'user.dir' will be set to the path where the LPhy script is.
     */
    @Override
    public Integer call() throws CommandLine.PicocliException { // business logic goes here...

        try {
            LPhyBeast lphyBeast = new LPhyBeast(infile, outfile, wd);

            lphyBeast.setRep(rep);
            lphyBeast.run(chainLength, preBurnin);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommandLine.PicocliException(e.toString());
        }
        return 0;
    }

    /**TODO not working
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