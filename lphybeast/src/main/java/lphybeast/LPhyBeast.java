package lphybeast;

import lphy.base.evolution.alignment.Alignment;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.codebuilder.CanonicalCodeBuilder;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.Value;
import lphy.core.parser.LPhyParserDictionary;
import lphy.core.simulator.NamedRandomValueSimulator;
import lphy.core.simulator.Sampler;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
            lPhyBeastConfig = new LPhyBeastConfig(infile, outfile, wd, null, null, false);
            lPhyBeastConfig.setMCMCConfig(chainLength, preBurnin, -1);
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
        // Ignoring the logging ability for the given lphy random variables
        final String[] varNotLog = lPhyBeastConfig.getVarNotLog();

        LPhyParserDictionary parserDictFinal;
        // check if it is model misspecification test
        Path lphyM2 = lPhyBeastConfig.getModel2File();
        if (lphyM2 == null) { // normal lphybeast run
            // must provide File lphyFile, int numReplicates, Long seed
            Map<Integer, List<Value>> allReps = simulator.simulateAndLog(lphyFile, filePathNoExt,
                    1, constants, varNotLog, null);

            parserDictFinal = simulator.getParserDictionary();

        } else { // model misspecification test
            // simulateAndLog and simulate use the same sampler, it can sample different lphy scripts.
            // M1 from File lphyFile, int numReplicates, Long seed
            Map<Integer, List<Value>> allRepsM1 = simulator.simulate(lphyFile,
                    1, constants, varNotLog, null);
            // original parserDict is M1
            LPhyParserDictionary parserDictM1 = simulator.getParserDictionary();

            // here is M2 using the file lphyM2
            Map<Integer, List<Value>> allRepsM2 = simulator.simulateAndLog(lphyM2.toFile(),
                    filePathNoExt+"_misspc", 1, constants, varNotLog, null);
            LPhyParserDictionary parserDictM2 = simulator.getParserDictionary();

            // log two XMLs:  Alignment1-Model1 and A2M2
            if (lPhyBeastConfig.log_orignal_xmls) {
                // due to Windows logging unicode issue, BEASTContext calls updateIDs(value) to update IDs
                // keep this line here, so IDs will be same between m1 and m2
                String xml1str = dictToBEASTXML(parserDictM1, filePathNoExt+"_a1m1");
                Path outFilePath1 = Path.of(filePathNoExt+"_a1m1.xml");
                // log m2 XML
                String xml2str = dictToBEASTXML(parserDictM2, filePathNoExt+"_a2m2");
                Path outFilePath2 = Path.of(filePathNoExt+"_a2m2.xml");

                writeXMLToFile(outFilePath1, xml1str);
                writeXMLToFile(outFilePath2, xml2str);
            }

            /*
             * Swapping the value of Value<Alignment> in lphy dict is easier than
             * swapping sequences between two beast2 alignment.
             * Note: it will be matter when swapping sequences between two beast2 alignments
             * without ensuring the taxa in the same order.
             */

            // pull out all Alignment and TimeTree
            List<Value<?>> alignmentsM1 = parserDictM1.getNamedValuesByType(Alignment.class);
            List<Value<?>> alignmentsM2 = parserDictM2.getNamedValuesByType(Alignment.class);

            List<Value<?>> timeTreesM1 = parserDictM1.getNamedValuesByType(TimeTree.class);
            List<Value<?>> timeTreesM2 = parserDictM2.getNamedValuesByType(TimeTree.class);

            // TODO: assuming all alignments and trees in M1 must be in M2 with the same ID.
            for (Value v1 : alignmentsM1) {

                Alignment a1 = (Alignment) v1.value();
                boolean processed = false;

                for (Value v2 : alignmentsM2) {

                    if (v1.getId().equals(v2.getId())) {
                        // fail if there is data clamping
                        if (parserDictM2.isClamped(v1.getId()))
                            throw new IllegalArgumentException("Model misspecification test does not support data clamping ! " +
                                    "Clamped alignment : " + v1.getId());

                        Alignment a2 = (Alignment) v2.value();

                        // validate all pairs of alignments to have the same taxa and sites
                        // Objects.equals(@Nullable, @Nullable)
                        if (!Objects.equals(a1.nchar(), a2.nchar()))
                            throw new IllegalArgumentException("Alignments must has the same length during model misspecification test ! " +
                                "\nModel 1 alignment " + v1.getId() + " nchar = " + a1.nchar() +
                                "\nModel 2 alignment " + v2.getId() + " nchar = " + a2.nchar());
                        String taxaNames1 = Arrays.stream(a1.getTaxaNames()).sorted().collect(Collectors.joining(","));
                        String taxaNames2 = Arrays.stream(a2.getTaxaNames()).sorted().collect(Collectors.joining(","));
                        if (! taxaNames1.equals(taxaNames2))
                            throw new IllegalArgumentException("Taxa names must be same during model misspecification test ! " +
                                    "\nModel 1 alignment " + v1.getId() + " has taxa : " + taxaNames1 +
                                    "\nModel 2 alignment " + v2.getId() + " has taxa : " + taxaNames2);

                        // replace the value inside Value, otherwise it will break Graph
                        v2.setValue(v1.value());

                        parserDictM2.getModelDictionary().put(v2.getId(), v2);
                        // TODO not sure this set will be used, but this add another D
//                        parserDictM2.getModelValues().add(v2);

                        processed = true;
                        break;
                    }

                } // End for loop

                if (!processed)
                    // TODO waring instead ?
                    throw new IllegalArgumentException("Model 1 alignment " + v1.getId() + " does not exist in model 2 !");

            }

            for (Value v1 : timeTreesM1) {

                TimeTree t1 = (TimeTree) v1.value();
                boolean processed = false;

                for (Value v2 : timeTreesM2) {

                    if (v1.getId().equals(v2.getId())) {
                        // fail if there is data clamping
                        if (parserDictM2.isClamped(v1.getId()))
                            throw new IllegalArgumentException("Model misspecification test does not support data clamping ! " +
                                    "Clamped alignment : " + v1.getId());

                        TimeTree t2 = (TimeTree) v2.value();

                        // validate all pairs of alignments to have the same taxa and sites
                        // Objects.equals(@Nullable, @Nullable)
                        if (!Objects.equals(t1.n(), t2.n()))
                            throw new IllegalArgumentException("TimeTree must has the same tips during model misspecification test ! " +
                                    "\nModel 1 TimeTree " + v1.getId() + " n tips = " + t1.n() +
                                    "\nModel 2 TimeTree " + v2.getId() + " n tips = " + t2.n());

                        // replace the value inside Value, otherwise it will break Graph
                        v2.setValue(v1.value());

                        parserDictM2.getModelDictionary().put(v2.getId(), v2);
                        // TODO not sure this set will be used, but this add another D
//                        parserDictM2.getModelValues().add(v2);

                        processed = true;
                        break;
                    }

                } // End for loop

                if (!processed)
                    // TODO waring instead ?
                    throw new IllegalArgumentException("Model 1 TimeTree " + v1.getId() + " does not exist in model 2 !");

            }

            parserDictFinal = parserDictM2;

        } // parserDictM2 is the final result to XML

//TODO        LoggerUtils.log.info("Replace alignment(s) : " +  + "\n, replace time tree(s) : " + + "\n");

        // create XML string from reader, given file name and MCMC setting
        String xml = dictToBEASTXML(parserDictFinal, filePathNoExt);

        writeXMLToFile(outPath, xml);
    }

    private void writeXMLToFile(Path outPath, String xml) throws IOException {
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

