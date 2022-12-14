package lphybeast;

import lphy.core.GraphicalLPhyParser;
import lphy.core.LPhyParser;
import lphy.core.Sampler;
import lphy.graphicalModel.RandomValueLogger;
import lphy.graphicalModel.logger.TreeFileLogger;
import lphy.graphicalModel.logger.VarFileLogger;
import lphy.parser.REPL;
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

    // LPhyBeastConfig to contain all settings
    final public LPhyBeastConfig lPhyBeastConfig;

    // register classes outside LPhyBeast, reduce loading time,
    // can be null, then initiate in BEASTContext.
    private final LPhyBEASTLoader loader;

    public LPhyBeast(LPhyBEASTLoader loader, LPhyBeastConfig lPhyBeastConfig) {
        this.loader = loader;
        this.lPhyBeastConfig = lPhyBeastConfig;
    }

    /**
     * Initiate LPhyBEASTLoader every LPhyBeast instance.
     * @param chainLength    if <=0, then use default 1,000,000.
     *                       logEvery = chainLength / numOfSamples,
     *                       where numOfSamples = 2000 as default.
     * @param preBurnin      preBurnin for BEAST MCMC, default to 0.
     */
    public LPhyBeast(long chainLength, int preBurnin, LPhyBeastConfig lPhyBeastConfig) {
        this(null, lPhyBeastConfig);
        lPhyBeastConfig.setChainLength(chainLength);
        lPhyBeastConfig.setPreBurnin(preBurnin);
    }

    /**
     * For unit test, and then call {@link #lphyStrToXML(String, String)}.
     */
    public LPhyBeast() {
        loader = null;
        try {
            lPhyBeastConfig = new LPhyBeastConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        int rep = lPhyBeastConfig.getRep();
        if (lPhyBeastConfig.inPath == null || lPhyBeastConfig.outPath == null || rep < 1 ||
                lPhyBeastConfig.getChainLength() < BEASTContext.NUM_OF_SAMPLES)
            throw new IllegalArgumentException("Illegal inputs : inPath = " + lPhyBeastConfig.inPath +
                    ", outPath = " + lPhyBeastConfig.outPath + ", rep = " + rep + ", chainLength = " +
                    lPhyBeastConfig.getChainLength() + ", preBurnin = " + lPhyBeastConfig.getPreBurnin());
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
            createXML(lPhyBeastConfig.outPath);
        }
    }

    private Path getOutPath(int i) {
        final String outPathNoExt = getPathNoExtension(lPhyBeastConfig.outPath);
        // update outPath to add i
        return Paths.get(outPathNoExt + "_" + i + ".xml");
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
        String xml = toBEASTXML(Objects.requireNonNull(reader), pathNoExt);
        writeXML(xml, outPath);
    }

    private BufferedReader lphyReader() throws FileNotFoundException {
        // need to call reader each loop
        FileReader fileReader = new FileReader(Objects.requireNonNull(lPhyBeastConfig.inPath).toFile());
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
     * @return    BEAST 2 XML
     * @see BEASTContext#toBEASTXML(String, long, int)
     * @throws IOException
     */
    private String toBEASTXML(BufferedReader reader, String filePathNoExt) throws IOException {
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
        BEASTContext context = new BEASTContext(parser, loader, lPhyBeastConfig);

        //*** Write BEAST 2 XML ***//
        // remove any dir in filePathNoExt here
        if (filePathNoExt.contains(File.separator))
            filePathNoExt = filePathNoExt.substring(filePathNoExt.lastIndexOf(File.separator)+1);

        // filePathNoExt here is file stem, which will be used in XML log file names.
        // Cannot handle any directories from other machines.
        return context.toBEASTXML(filePathNoExt);
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

        return toBEASTXML(Objects.requireNonNull(reader), fileNameStem);
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

