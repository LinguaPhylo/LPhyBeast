package lphybeast;

import beast.base.evolution.datatype.DataType;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import jebl.evolution.sequences.SequenceType;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.Value;
import lphy.util.LoggerUtils;
import lphybeast.spi.LPhyBEASTExt;
import lphybeast.tobeast.operators.DefaultTreeOperatorStrategy;
import lphybeast.tobeast.operators.TreeOperatorStrategy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static beast.pkgmgmt.BEASTClassLoader.addServices;

/**
 * The factory class to load LPhyBEAST extensions using {@link PackageManager}.
 * All distributions, functions and data types will be collected
 * in this class for later use.
 *
 * @author Walter Xie
 */
public class LPhyBEASTLoader {
    private static LPhyBEASTLoader factory;

    private LPhyBEASTLoader() {
        // ServiceLoader cannot work with BEASTClassLoader
        registerExtensions(null);
    }

    // singleton
    public static synchronized LPhyBEASTLoader getInstance() {
        if (factory == null)
            factory = new LPhyBEASTLoader();
        return factory;
    }

    //*** registry ***//

    /**
     * {@link ValueToBEAST}
     */
    public List<ValueToBEAST> valueToBEASTList;
    /**
     * Use LinkedHashMap to keep inserted ordering, so the first matching converter is used.
     * @see  GeneratorToBEAST
     */
    public Map<Class, GeneratorToBEAST> generatorToBEASTMap;
    /**
     * LPhy sequence types {@link SequenceType} maps to BEAST {@link DataType}
     */
    public Map<SequenceType, DataType> dataTypeMap;
    /**
     * {@link Generator}
     */
    public List<Class<? extends Generator>> excludedGeneratorClasses;
    /**
     * {@link Value}
     */
    public List<Class<? extends Value>> excludedValueClasses;
    /**
     * Not {@link DefaultTreeOperatorStrategy}
     */
    public List<TreeOperatorStrategy> newTreeOperatorStrategies;

    public static final String LPHY_BEAST_EXT = "lphybeast.spi.LPhyBEASTExt";


    /**
     * register version.xml and add services when using IDE,
     * before creating an instance of LPhyBEAST loader.
     * @param versionFiles array of version.xml in each B2 package.
     */
    public static void addBEAST2Services(String[] versionFiles) {
        if (versionFiles != null) {
            try {
                // This line should only be called using IDE:
                // this loads all jars in B2 pkgs from local B2 repo folder
                // e.g. ~/Library/Application\ Support/BEAST/2.7/
                PackageManager.loadExternalJars(); // TODO to improve: without involving local installed pkgs.
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (String vf : versionFiles)
                addServices(vf);
        }
    }

    /**
     * Use {@link PackageManager} to load the container classes from LPhyBEAST extensions,
     * which include all extended classes.
     * @return  the list of container classes (one per extension).
     */
    public List<LPhyBEASTExt> getExtClasses() throws IOException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        Map<String, Set<String>> providers = BEASTClassLoader.getServices();
        Set<String> extStr = providers.get(LPHY_BEAST_EXT);

        if (extStr==null || extStr.isEmpty())
            throw new IllegalArgumentException("Cannot find the BEAST2 service implementing " + LPHY_BEAST_EXT + " !");

        List<LPhyBEASTExt> extensionList = new ArrayList<>();
        for (String clsStr : extStr) {
            // get beast service
            Class<?> cls = BEASTClassLoader.forName(clsStr, LPHY_BEAST_EXT);
            // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
            try {
                Object obj = cls.getDeclaredConstructor().newInstance();
                extensionList.add((LPhyBEASTExt) obj);
            } catch (InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                // do nothing
            }
        }
//        catch (Throwable e) { e.printStackTrace(); }

        return extensionList;
    }


    //    private void registerExtensions(ServiceLoader<LPhyBEASTExt> loader, String clsName) {
    private void registerExtensions(List<String> spiClsNames) {
        valueToBEASTList = new ArrayList<>();
        generatorToBEASTMap = new LinkedHashMap<>();
        dataTypeMap = new ConcurrentHashMap<>();

        excludedGeneratorClasses = new ArrayList<>();
        excludedValueClasses = new ArrayList<>();

        newTreeOperatorStrategies = new ArrayList<>();

        try {
//            Iterator<LPhyBEASTExt> extensions = loader.iterator();
//            while (extensions.hasNext()) { // TODO validation if add same name

            List<LPhyBEASTExt> extList = null;
            try {
                try {
                    extList = getExtClasses();
                } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                         IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (LPhyBEASTExt ext : extList) {
                //*** LPhyBEASTExtImpl must have a public no-args constructor ***//
//                LPhyBEASTExt ext = extensions.next();
                // clsName == null then register all
                if (spiClsNames == null || spiClsNames.contains(ext.getClass().getName())) {
                    System.out.println("Registering extension from " + ext.getClass().getName());

                    final List<Class<? extends ValueToBEAST>> valuesToBEASTs = ext.getValuesToBEASTs();
                    final List<Class<? extends GeneratorToBEAST>> generatorToBEASTs = ext.getGeneratorToBEASTs();
                    final Map<SequenceType, DataType> dataTypeMap = ext.getDataTypeMap();

                    registerValueToBEAST(valuesToBEASTs);
                    registerGeneratorToBEAST(generatorToBEASTs);
                    registerDataTypes(dataTypeMap);

                    excludedGeneratorClasses.addAll(ext.getExcludedGenerator());
                    excludedValueClasses.addAll(ext.getExcludedValue());

                    if ( ! (ext.getTreeOperatorStrategy() instanceof DefaultTreeOperatorStrategy) )
                        newTreeOperatorStrategies.add(ext.getTreeOperatorStrategy());
                }
            }

            System.out.println(valueToBEASTList.size() + " ValuesToBEAST = " + valueToBEASTList);
            System.out.println(generatorToBEASTMap.size() + " GeneratorToBEAST = " + generatorToBEASTMap);
            System.out.println(dataTypeMap.size() + " Data Type = " + dataTypeMap);
            System.out.println(excludedGeneratorClasses.size() + " extra Generator(s) excluded = " + excludedGeneratorClasses);
            System.out.println(excludedValueClasses.size() + " extra Value(s) excluded = " + excludedValueClasses);
            System.out.println(newTreeOperatorStrategies.size() + " new Tree Operator Strategies = " + newTreeOperatorStrategies);

        } catch (ServiceConfigurationError serviceError) {
            System.err.println(serviceError);
            serviceError.printStackTrace();
        }

    }

    private void registerValueToBEAST(final List<Class<? extends ValueToBEAST>> valuesToBEASTs) {
        for (Class<? extends ValueToBEAST> c : valuesToBEASTs) {
            try {
                // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
                ValueToBEAST<?,?> valueToBEAST = (ValueToBEAST<?,?>) c.getDeclaredConstructor().newInstance();
                if (this.valueToBEASTList.contains(valueToBEAST))
                    LoggerUtils.log.warning(valueToBEAST + " exists in register, overwrite previous one !");
                this.valueToBEASTList.add(valueToBEAST);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerGeneratorToBEAST(final List<Class<? extends GeneratorToBEAST>> generatorToBEASTs) {
        for (Class<? extends GeneratorToBEAST> c : generatorToBEASTs) {
            try {
                // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
                GeneratorToBEAST<?,?> generatorToBEAST = (GeneratorToBEAST<?,?>) c.getDeclaredConstructor().newInstance();
                if (this.generatorToBEASTMap.containsKey(generatorToBEAST))
                    LoggerUtils.log.warning(generatorToBEAST + " exists in register, overwrite previous one !");
                this.generatorToBEASTMap.put(generatorToBEAST.getGeneratorClass(), generatorToBEAST);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerDataTypes(final Map<SequenceType, DataType> dataTypeMap) {
        for (Map.Entry<SequenceType, DataType> entry : dataTypeMap.entrySet()) {
            if (this.dataTypeMap.containsKey(entry.getKey()))
                LoggerUtils.log.warning(entry.getKey() + " exists in register, overwrite previous one !");
            this.dataTypeMap.put(entry.getKey(), entry.getValue());
        }
    }

}
