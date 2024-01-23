package lphybeast;

import lphy.core.codebuilder.CanonicalCodeBuilder;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.Value;
import lphy.core.parser.LPhyParserDictionary;
import lphy.core.simulator.NamedRandomValueSimulator;
import lphy.core.simulator.Sampler;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
                lPhyBeastConfig.getChainLength() < LPhyBeastConfig.NUM_OF_SAMPLES)
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
//                BufferedReader reader = lphyReader();
                // need new reader
                writeXMLFrom();
            }
        } else { // 1 simulation
//            BufferedReader reader = lphyReader();
            writeXMLFrom();
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

    // the relative path given in readNexus in a script always refers to user.dir
    // out path without file extension for output file name,
    // and XML loggers after removing the parent dir.
    private void writeXMLFrom() throws IOException {
        final File lphyFile = Objects.requireNonNull(lPhyBeastConfig.inPath).toFile();
        Path outPath = getXMLFilePath();
        // outPath may be added i
        final String filePathNoExt = lPhyBeastConfig.getOutPathNoExtension(outPath);

        NamedRandomValueSimulator simulator = new NamedRandomValueSimulator();
        // constants are inputted by user for Macro
        final String[] constants = lPhyBeastConfig.getLphyConst();
        // must provide File lphyFile, int numReplicates, Long seed
        Map<Integer, List<Value>> allReps = simulator.simulateAndLog(lphyFile, filePathNoExt,
                1, constants, null, null);

        LPhyParserDictionary parserDict = simulator.getParserDictionary();
        // create XML string from reader, given file name and MCMC setting
        String xml = dictToBEASTXML(parserDict, filePathNoExt);

        FileWriter fileWriter = new FileWriter(Objects.requireNonNull(outPath).toFile());
        PrintWriter writer = new PrintWriter(fileWriter);
        writer.println(xml);
        writer.flush();
        writer.close();

        LoggerUtils.log.info("Save BEAST 2 XML to " + outPath.toAbsolutePath() + "\n\n");
    }

    /**
     * Alternative method to give LPhy script (e.g. from String), not only from a file.
     * @param parserDictionary
     * @param filePathNoExt  file path but without extension
     * @return    BEAST 2 XML
     * @see BEASTContext#toBEASTXML(String)
     * @throws IOException
     */
    private String dictToBEASTXML(LPhyParserDictionary parserDictionary, String filePathNoExt) throws IOException {

        // Add lphy script to the top of XML
        CanonicalCodeBuilder canonicalCodeBuilder = new CanonicalCodeBuilder();
        StringBuilder xmlBuilder = new StringBuilder();
        // this has to be the 1st line in the file
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>").append("\n");
        // lphy code in comment block
        String codeBlock = canonicalCodeBuilder.getCode(parserDictionary);
        xmlBuilder.append("<!--\n").append(codeBlock).append("\n-->");

        // register parser, pass cached loader
        BEASTContext context = new BEASTContext(parserDictionary, loader, lPhyBeastConfig);

        //*** Write BEAST 2 XML ***//
        // remove any dir in filePathNoExt here
        final String logFileStem = lPhyBeastConfig.rmParentDir(filePathNoExt);
        // filePathNoExt here is file stem, which will be used in XML log file names.
        // Cannot handle any directories from other machines.
        String xml = context.toBEASTXML(logFileStem);
        // rm it generated by beast2
        xml = xml.replaceAll("<\\?xml [^>]*\\?>", "");
        xmlBuilder.append("\n").append(xml).append("\n");
        return xmlBuilder.toString();
    }

    /**
     * Parse LPhy script into BEAST 2 XML in string. Only used for tests.
     * @param lphy           LPhy script in string, and one line one command.
     * @param fileNameStem   output file stem without file extension
     * @return  BEAST 2 XML in string
     */
    public String lphyStrToXML(String lphy, String fileNameStem) throws IOException {
        // no output no replicates
        Sampler sampler = Sampler.createSampler(lphy);
        LPhyParserDictionary parserDict = sampler.getParserDictionary();
        return dictToBEASTXML(parserDict, fileNameStem);
    }



    private BufferedReader lphyReader() throws FileNotFoundException {
        // need to call reader each loop
        FileReader fileReader = new FileReader(Objects.requireNonNull(lPhyBeastConfig.inPath).toFile());
        return new BufferedReader(fileReader);
    }

    // 1st is id, 2nd is value in string, otherwise null.
    // line looks like : n = 50;
    private String[] parse(String line) {
        // trim all spaces, TODO not working for string
        String[] idVal = line.trim().split("=");
        if (idVal.length != 2)
            return null;
        idVal[0] = idVal[0].trim();
        idVal[1] = idVal[1].replace(";", "").trim();
        return idVal;
    }


}

