package lphybeast;

import lphy.system.UserDir;
import lphy.util.LoggerUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class LPhyBeastConfig {

    public final Path inPath;
    public final Path outPath;
//    public final Path wd;//TODO currently using UserDir.set/getUserDir
    final public boolean compressConstantSites;
    final public String alignmentId;
    final public boolean logAllAlignments;

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
    private int repId = -1; // >=0 for multi-outputs

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
     */
    public LPhyBeastConfig(Path infile, Path outfile, Path wd,
                           boolean compressConstantSites, String alignmentId, boolean logAllAlignments)
            throws IOException {

        this.compressConstantSites = compressConstantSites;
        this.alignmentId = alignmentId;
        this.logAllAlignments = logAllAlignments;

        if (infile == null || !infile.toFile().exists())
            throw new IOException("Cannot find LPhy script file ! " + (infile != null ? infile.toAbsolutePath() : null));
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
        this.compressConstantSites = false;
        this.alignmentId = null;
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

    public int getPreBurnin() {
        return preBurnin;
    }

    public void setPreBurnin(int preBurnin) {
        this.preBurnin = preBurnin;
    }

    public long getChainLength() {
        return chainLength;
    }

    public void setChainLength(long chainLength) {
        this.chainLength = chainLength;
    }

    public int getRepId() {
        return repId;
    }

    /**
     * @param repId    the index of replicates of simulations, >= 0.
     */
    public void setRepId(int repId) {
        if (repId >= 0) this.repId = repId;
    }
}
