package lphybeast;

import lphy.core.GraphicalLPhyParser;
import lphy.core.LPhyMetaParser;
import lphy.core.Sampler;
import lphy.graphicalModel.RandomValueLogger;
import lphy.graphicalModel.code.CanonicalCodeBuilder;
import lphy.graphicalModel.logger.TreeFileLogger;
import lphy.graphicalModel.logger.VarFileLogger;
import lphy.parser.REPL;
import lphy.util.LoggerUtils;

import java.io.*;
import java.nio.file.Path;
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

    final int repTot;

    // LPhyBeastConfig to contain all settings
    final public LPhyBeastConfig lPhyBeastConfig;

    // register classes outside LPhyBeast, reduce loading time,
    // can be null, then initiate in BEASTContext.
    private final LPhyBEASTLoader loader;

    public LPhyBeast(LPhyBEASTLoader loader, LPhyBeastConfig lPhyBeastConfig, int repTot) {
        this.loader = loader;
        this.lPhyBeastConfig = lPhyBeastConfig;
        this.repTot = repTot;
    }

    /**
     * For unit test, and then call {@link #lphyStrToXML(String, String)}.
     * chainLength uses default 1,000,000. numOfSamples = 2,000.
     * preBurnin for BEAST MCMC set to 0.
     */
    public LPhyBeast() {
        repTot = 1;
        loader = null;// LPhyBEASTLoader.getInstance();
        try {
            lPhyBeastConfig = new LPhyBeastConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create LPhyBeast instance from LPhyBeastLauncher in another subproject.
     */
    public LPhyBeast(Path infile, Path outfile, Path wd, long chainLength, int preBurnin) {
        repTot = 1;
        loader = null;
        try {
            lPhyBeastConfig = new LPhyBeastConfig(infile, outfile, wd, 0, false, false);
            lPhyBeastConfig.setChainLength(chainLength);
            lPhyBeastConfig.setPreBurnin(preBurnin);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        if (lPhyBeastConfig.inPath == null || lPhyBeastConfig.outPath == null || repTot < 1 ||
                lPhyBeastConfig.getChainLength() < BEASTContext.NUM_OF_SAMPLES)
            throw new IllegalArgumentException("Illegal inputs : inPath = " + lPhyBeastConfig.inPath +
                    ", outPath = " + lPhyBeastConfig.outPath + ", rep = " + repTot + ", chainLength = " +
                    lPhyBeastConfig.getChainLength() + ", preBurnin = " + lPhyBeastConfig.getPreBurnin());
        try {
            run(repTot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * run all replicates if repTot > 1
     * @param repTot  number of replicates to run
     * @throws IOException
     */
    public void run(int repTot) throws IOException {
        // e.g. well-calibrated validations
        if (repTot > 1) {
            LoggerUtils.log.info("\nStart " + repTot + " replicates : \n");

            for (int i = 0; i < repTot; i++) {
                // add _i after file stem
                lPhyBeastConfig.setRepId(i);
                BufferedReader reader = lphyReader();
                // need new reader
                writeXMLFrom(reader);
            }
        } else { // 1 simulation
            BufferedReader reader = lphyReader();
            writeXMLFrom(reader);
        }
    }

    private Path getXMLFilePath() {
        int repId = lPhyBeastConfig.getRepId();
        if ( (repId >= 0 && repTot <= 1) || (repId < 0 && repTot > 1) ) {
            throw new IllegalArgumentException("The replicate index (" + repId +
                    ") does not match the total replicates (" + repTot + ") ! " );
        }

        if (repId >= 0) {
            // add _i after file stem
            return lPhyBeastConfig.getXMLFilePathWithRepId();
        } else {
            return lPhyBeastConfig.outPath;
        }
    }

    private BufferedReader lphyReader() throws FileNotFoundException {
        // need to call reader each loop
        FileReader fileReader = new FileReader(Objects.requireNonNull(lPhyBeastConfig.inPath).toFile());
        return new BufferedReader(fileReader);
    }

    // the relative path given in readNexus in a script always refers to user.dir
    // out path without file extension for output file name,
    // and XML loggers after removing the parent dir.
    private void writeXMLFrom(BufferedReader reader) throws IOException {
        Path outPath = getXMLFilePath();
        // outPath may be added i
        final String filePathNoExt = lPhyBeastConfig.getOutPathNoExtension(outPath);

        // create XML string from reader, given file name and MCMC setting
        String xml = toBEASTXML(Objects.requireNonNull(reader), filePathNoExt);

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
     * @param filePathNoExt  file path but without extension
     * @return    BEAST 2 XML
     * @see BEASTContext#toBEASTXML(String)
     * @throws IOException
     */
    private String toBEASTXML(BufferedReader reader, String filePathNoExt) throws IOException {
        //*** Parse LPhy file ***//
        LPhyMetaParser parser = new REPL();
        parser.source(reader);

        // Add lphy script to the top of XML
        CanonicalCodeBuilder canonicalCodeBuilder = new CanonicalCodeBuilder();
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<!--\n").append(canonicalCodeBuilder.getCode(parser)).append("\n-->\n");

        // log true values and tree
        List<RandomValueLogger> loggers = new ArrayList<>();
        final String filePathNoExtTrueVaule = filePathNoExt + "_" + "true";
        loggers.add(new VarFileLogger(filePathNoExtTrueVaule, true, true));
        loggers.add(new TreeFileLogger(filePathNoExtTrueVaule));
        //TODO no AlignmentFileLogger?

        GraphicalLPhyParser gparser = new GraphicalLPhyParser(parser);
        Sampler sampler = new Sampler(gparser);
        sampler.sample(1, loggers);

        // register parser, pass cached loader
        BEASTContext context = new BEASTContext(parser, loader, lPhyBeastConfig);

        //*** Write BEAST 2 XML ***//
        // remove any dir in filePathNoExt here
        final String logFileStem = lPhyBeastConfig.rmParentDir(filePathNoExt);
        // filePathNoExt here is file stem, which will be used in XML log file names.
        // Cannot handle any directories from other machines.
        xmlBuilder.append("\n").append(context.toBEASTXML(logFileStem)).append("\n");
        return xmlBuilder.toString();
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



    //    private static void source(BufferedReader reader, LPhyMetaParser parser)
//            throws IOException {
//        LPhyMetaParser.Context mode = null;
//
//        String line = reader.readLine();
//        while (line != null) {
//            String s = line.replaceAll("\\s+","");
//            if (s.isEmpty()) {
//                // skip empty lines
//            } else if (s.startsWith("data{"))
//                mode = LPhyMetaParser.Context.data;
//            else if (s.startsWith("model{"))
//                mode = LPhyMetaParser.Context.model;
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

