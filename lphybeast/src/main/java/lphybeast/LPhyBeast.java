package lphybeast;

import lphy.core.GraphicalLPhyParser;
import lphy.core.LPhyParser;
import lphy.core.Sampler;
import lphy.graphicalModel.RandomValueLogger;
import lphy.graphicalModel.logger.TreeFileLogger;
import lphy.graphicalModel.logger.VarFileLogger;
import lphy.parser.REPL;
import lphy.system.UserDir;
import lphy.util.LoggerUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Main class to set up a simulation or simulations.
 * This should be swing/awt free.
 * @author Walter Xie
 * @author Alexei Dummond
 */
public class LPhyBeast implements Runnable {

    private final Path inPath;
    private final Path outPath;
    private int preBurnin = -1; // auto estimate
    private long chainLength = 1000000;

    private int rep = 1; // for multi-outputs

    private boolean addConstantSites;

    // register classes outside LPhyBeast, reduce loading time,
    // can be null, then initiate in BEASTContext.
    private final LPhyBEASTLoader loader;

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
     * @param chainLength   The total chain length of MCMC, default to 1 million.
     * @param preBurnin     The number of burnin samples taken before entering the main loop of MCMC.
     *                      If < 0, as default, then estimate it based on all state nodes size.
     * @throws IOException
     */
    public LPhyBeast(Path infile, Path outfile, Path wd, long chainLength, int preBurnin,
                     LPhyBEASTLoader loader) throws IOException {
        this.chainLength = chainLength;
        this.preBurnin = preBurnin;
        this.loader = loader;

        if (infile == null || !infile.toFile().exists())
            throw new IOException("Cannot find LPhy script file ! " + (infile != null ? infile.toAbsolutePath() : null));
        String fileName = infile.getFileName().toString();
        if (!fileName.endsWith(".lphy"))
            throw new IllegalArgumentException("Invalid LPhy file: the postfix has to be '.lphy'");

        if (wd != null)
            UserDir.setUserDir(wd.toAbsolutePath().toString());
        // if the relative path, then concatenate user.dir before it
        inPath = UserDir.getUserPath(infile);
        // still need to set user.dir, if no -wd, in case LPhy script uses relative path
        if (wd == null)
            // set user.dir to the folder containing lphy script
            UserDir.setUserDir(inPath.getParent().toString());

        LoggerUtils.log.info("Read LPhy script from " + inPath.toAbsolutePath() + "\n");

        if (outfile != null) {
            outPath = UserDir.getUserPath(outfile);
        } else {
            String infileNoExt = getFileStem(inPath);
            // add wd before file stem
            outPath = Paths.get(UserDir.getUserDir().toString(), infileNoExt + ".xml");
        }

    }

    /**
     * Initiate LPhyBEASTLoader every LPhyBeast instance.
     * @see #LPhyBeast(Path, Path, Path, long, int, LPhyBEASTLoader)
     */
    public LPhyBeast(Path infile, Path outfile, Path wd, long chainLength, int preBurnin) throws IOException {
        this(infile, outfile, wd, chainLength,  preBurnin, null);
    }

    /**
     * For unit test, and then call {@link #lphyStrToXML(String, String)}.
     * @see #LPhyBeast(Path, Path, Path, long, int)
     */
    public LPhyBeast() {
        inPath = null; // lphy script is in String
        outPath = null;
        preBurnin = 0;
        loader = null;
    }

    public void setAddConstantSites(boolean addConstantSites) {
        this.addConstantSites = addConstantSites;
    }

    /**
     * @param rep      replicates of simulations, >= 1.
     */
    public void setRep(int rep) {
        if (rep > 1) this.rep = rep; // default to 1
    }

    @Override
    public void run() {
        if (inPath == null || outPath == null || rep < 1 || chainLength < BEASTContext.NUM_OF_SAMPLES)
            throw new IllegalArgumentException("Illegal inputs : inPath = " + inPath + ", outPath = " + outPath +
                    ", rep = " + rep + ", chainLength = " + chainLength + ", preBurnin = " + preBurnin);
        try {
            run(rep);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(int rep) throws IOException {
        // e.g. well-calibrated validations
        if (rep > 1) {
            LoggerUtils.log.info("\nStart " + rep + " replicates : \n");

            for (int i = 0; i < rep; i++) {
                // add _i after file stem
                Path outPathPerRep = getOutPath(i);
                // need new reader
                createXML(outPathPerRep);
            }
        } else { // 1 simulation
            createXML(outPath);
        }
    }

    private Path getOutPath(int i) {
        final String outPathNoExt = getPathNoExtension(outPath);
        // update outPath to add i
        return Paths.get(outPathNoExt + "_" + i + ".xml");
    }

    private String getFileStem(Path path) {
        String fileName = Objects.requireNonNull(path).getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private String getPathNoExtension(Path path) {
        String str = Objects.requireNonNull(path).toString();
        return str.substring(0, str.lastIndexOf("."));
    }

    // the relative path given in readNexus in a script always refers to user.dir
    // fileNameStem for both outfile and XML loggers
    private void createXML(Path outPath) throws IOException {
        BufferedReader reader = lphyReader();

        final String pathNoExt = getPathNoExtension(outPath);
        // create XML string from reader, given file name and MCMC setting
        String xml = toBEASTXML(Objects.requireNonNull(reader), pathNoExt, chainLength, preBurnin);
        writeXML(xml, outPath);
    }

    private BufferedReader lphyReader() throws FileNotFoundException {
        // need to call reader each loop
        FileReader fileReader = new FileReader(Objects.requireNonNull(inPath).toFile());
        return new BufferedReader(fileReader);
    }

    private void writeXML(String xml, Path outPath) throws IOException {
        FileWriter fileWriter = new FileWriter(Objects.requireNonNull(outPath).toFile());
        PrintWriter writer = new PrintWriter(fileWriter);
        writer.println(xml);
        writer.flush();
        writer.close();

        LoggerUtils.log.info("Save BEAST 2 XML to " + outPath.toAbsolutePath() + "\n\n");
    }


    /**
     * Alternative method to give LPhy script (e.g. from String), not only from a file.
     * @param reader         BufferedReader
     * @param filePathNoExt  no file extension
     * @param chainLength    if <=0, then use default 1,000,000.
     *                       logEvery = chainLength / numOfSamples,
     *                       where numOfSamples = 2000 as default.
     * @param preBurnin      preBurnin for BEAST MCMC, default to 0.
     * @return    BEAST 2 XML
     * @see BEASTContext#toBEASTXML(String, long, int)
     * @throws IOException
     */
    private String toBEASTXML(BufferedReader reader, String filePathNoExt, long chainLength,
                              int preBurnin) throws IOException {
        //*** Parse LPhy file ***//
        LPhyParser parser = new REPL();
        parser.source(reader);

        // log true values and tree
        List<RandomValueLogger> loggers = new ArrayList<>();
        final String filePathNoExtTrueVaule = filePathNoExt + "_" + "true";
        loggers.add(new VarFileLogger(filePathNoExtTrueVaule, true, true));
        loggers.add(new TreeFileLogger(filePathNoExtTrueVaule));

        GraphicalLPhyParser gparser = new GraphicalLPhyParser(parser);
        Sampler sampler = new Sampler(gparser);
        sampler.sample(1, loggers);

        // register parser, pass cached loader
        BEASTContext context = new BEASTContext(parser, loader);
        context.setAddConstantSites(addConstantSites);

        //*** Write BEAST 2 XML ***//
        // remove any dir in filePathNoExt here
        if (filePathNoExt.contains(File.separator))
            filePathNoExt = filePathNoExt.substring(filePathNoExt.lastIndexOf(File.separator)+1);

        // filePathNoExt here is file stem, which will be used in XML log file names.
        // Cannot handle any directories from other machines.
        return context.toBEASTXML(filePathNoExt, chainLength, preBurnin);
    }

    /**
     * Parse LPhy script into BEAST 2 XML in string. Only used for tests.
     * @param lphy           LPhy script in string, and one line one command.
     * @param fileNameStem   output file stem without file extension
     * @return  BEAST 2 XML in string
     */
    public String lphyStrToXML(String lphy, String fileNameStem) throws IOException {
        Reader inputString = new StringReader(lphy);
        BufferedReader reader = new BufferedReader(inputString);

        return toBEASTXML(Objects.requireNonNull(reader), fileNameStem, chainLength, preBurnin);
    }



    //    private static void source(BufferedReader reader, LPhyParser parser)
//            throws IOException {
//        LPhyParser.Context mode = null;
//
//        String line = reader.readLine();
//        while (line != null) {
//            String s = line.replaceAll("\\s+","");
//            if (s.isEmpty()) {
//                // skip empty lines
//            } else if (s.startsWith("data{"))
//                mode = LPhyParser.Context.data;
//            else if (s.startsWith("model{"))
//                mode = LPhyParser.Context.model;
//            else if (s.startsWith("}"))
//                mode = null; // reset
//            else {
//                if (mode == null)
//                    throw new IllegalArgumentException("Please use data{} to define data and " +
//                            "model{} to define models !\n" + line);
//
//                parser.parse(line, mode);
//            }
//            line = reader.readLine();
//        }
//        reader.close();
//    }


}

