package lphybeast;

import beast.base.evolution.datatype.DataType;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import jebl.evolution.sequences.SequenceType;
import lphy.core.logger.LoggerUtils;
import lphy.core.model.Generator;
import lphy.core.model.Value;
import lphybeast.spi.*;
import lphybeast.tobeast.operators.DefaultTreeOperatorStrategy;
import lphybeast.tobeast.operators.TreeOperatorStrategy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public List<Class> excludedValueTypes;
    /**
     * Not {@link DefaultTreeOperatorStrategy}
     */
    public List<TreeOperatorStrategy> newTreeOperatorStrategies;

    //*** new SPI registries ***//

    public List<MCMCStrategy> mcmcStrategies;
    public List<ValueHandler> valueHandlers;
    public List<TreeLikelihoodStrategy> treeLikelihoodStrategies;
    public List<OperatorContributor> operatorContributors;
    public List<AlignmentHandler> alignmentHandlers;

    /**
     * Maps each registered generator class to the extension that provided it.
     * Used for diagnostics when a mapping is missing.
     */
    private Map<Class<?>, String> generatorSources;
    /**
     * Names of all loaded extension classes.
     */
    private List<String> loadedExtensions;

    public static final String LPHY_BEAST_EXT = "lphybeast.spi.LPhyBEASTMapping";


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

            for (String vf : versionFiles) {
                // check if it exists before add
                Path path = Paths.get(vf);
                if (! path.toFile().exists())
                    throw new IllegalArgumentException("Cannot find the provided " + path.toAbsolutePath());
                addServices(vf);
                System.out.println("Adding BEAST2 services from " + path.toAbsolutePath());
            }
        }
        // Trigger classpath + BEAST_PACKAGE_PATH scanning for version.xml
        // files embedded in dependency JARs (e.g. beast-base).
        // Downstream projects must set BEAST_PACKAGE_PATH to include
        // the beast-base JAR (or its target/classes directory).
        BEASTClassLoader.initServices();
    }

    /**
     * Convenience method for extension module tests.
     * Loads core services from version.xml relative to the given directory
     * (typically {@code ../lphybeast} from an extension module's working directory).
     *
     * @param coreModuleDir path to the core lphybeast module directory containing version.xml
     */
    public static void loadServicesForTest(String coreModuleDir) {
        Path vfPath = Paths.get(coreModuleDir, "version.xml");
        if (!java.nio.file.Files.exists(vfPath))
            throw new IllegalArgumentException("Can't find LPhyBeast version.xml at: " + vfPath.toAbsolutePath());
        addBEAST2Services(new String[]{vfPath.toAbsolutePath().toString()});
    }

    /**
     * Use {@link PackageManager} to load the container classes from LPhyBEAST extensions,
     * which include all extended classes.
     * @return  the list of container classes (one per extension).
     */
    public List<LPhyBEASTMapping> getExtClasses() throws IOException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        Map<String, Set<String>> providers = BEASTClassLoader.getServices();
        Set<String> extStr = providers.get(LPHY_BEAST_EXT);

        if (extStr==null || extStr.isEmpty())
            throw new IllegalArgumentException("Cannot find the BEAST2 service implementing " + LPHY_BEAST_EXT + " !");

        List<LPhyBEASTMapping> extensionList = new ArrayList<>();
        for (String clsStr : extStr) {
            // get beast service
            Class<?> cls = BEASTClassLoader.forName(clsStr, LPHY_BEAST_EXT);
            // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
            try {
                Object obj = cls.getDeclaredConstructor().newInstance();
                extensionList.add((LPhyBEASTMapping) obj);
            } catch (InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                // do nothing
            }
        }
//        catch (Throwable e) { e.printStackTrace(); }

        return extensionList;
    }


    //    private void registerExtensions(ServiceLoader<LPhyBEASTMapping> loader, String clsName) {
    private void registerExtensions(List<String> spiClsNames) {
        valueToBEASTList = new ArrayList<>();
        generatorToBEASTMap = new LinkedHashMap<>();
        dataTypeMap = new ConcurrentHashMap<>();

        excludedGeneratorClasses = new ArrayList<>();
        excludedValueTypes = new ArrayList<>();

        newTreeOperatorStrategies = new ArrayList<>();

        // New SPI registries
        mcmcStrategies = new ArrayList<>();
        valueHandlers = new ArrayList<>();
        treeLikelihoodStrategies = new ArrayList<>();
        operatorContributors = new ArrayList<>();
        alignmentHandlers = new ArrayList<>();

        generatorSources = new LinkedHashMap<>();
        loadedExtensions = new ArrayList<>();

        try {
//            Iterator<LPhyBEASTMapping> extensions = loader.iterator();
//            while (extensions.hasNext()) { // TODO validation if add same name

            List<LPhyBEASTMapping> extList = null;
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

            for (LPhyBEASTMapping ext : extList) {
                //*** LPhyBEASTMappingImpl must have a public no-args constructor ***//
//                LPhyBEASTMapping ext = extensions.next();
                // clsName == null then register all
                if (spiClsNames == null || spiClsNames.contains(ext.getClass().getName())) {
                    String extName = ext.getClass().getName();
                    System.out.println("Registering extension from " + extName);
                    loadedExtensions.add(extName);

                    final List<Class<? extends ValueToBEAST>> valuesToBEASTs = ext.getValuesToBEASTs();
                    final List<Class<? extends GeneratorToBEAST>> generatorToBEASTs = ext.getGeneratorToBEASTs();
                    final Map<SequenceType, DataType> dataTypeMap = ext.getDataTypeMap();

                    registerValueToBEAST(valuesToBEASTs);
                    registerGeneratorToBEAST(generatorToBEASTs, extName);
                    registerDataTypes(dataTypeMap);

                    excludedGeneratorClasses.addAll(ext.getExcludedGenerator());
                    excludedValueTypes.addAll(ext.getExcludedValueType());

                    if ( ! (ext.getTreeOperatorStrategy() instanceof DefaultTreeOperatorStrategy) )
                        newTreeOperatorStrategies.add(ext.getTreeOperatorStrategy());
                }
            }

            // Discover new SPI services
            discoverServices(MCMCStrategy.class, mcmcStrategies);
            discoverServices(ValueHandler.class, valueHandlers);
            discoverServices(TreeLikelihoodStrategy.class, treeLikelihoodStrategies);
            discoverServices(OperatorContributor.class, operatorContributors);
            discoverServices(AlignmentHandler.class, alignmentHandlers);

            System.out.println("Load " + valueToBEASTList.size() + " ValuesToBEAST = " + valueToBEASTList);
            System.out.println("Load " + generatorToBEASTMap.size() + " GeneratorToBEAST = " + generatorToBEASTMap);
            System.out.println("Map " + dataTypeMap.size() + " data type(s) = " + dataTypeMap);
            System.out.println("Exclude " + excludedGeneratorClasses.size() + " extra Generator(s) = " + excludedGeneratorClasses);
            System.out.println("Exclude " + excludedValueTypes.size() + " extra Value(s) = " + excludedValueTypes);
            System.out.println("Load " + newTreeOperatorStrategies.size() + " new Tree Operator Strategies = " + newTreeOperatorStrategies);
            System.out.println("Load " + mcmcStrategies.size() + " MCMCStrategy(s), " +
                    valueHandlers.size() + " ValueHandler(s), " +
                    treeLikelihoodStrategies.size() + " TreeLikelihoodStrategy(s), " +
                    operatorContributors.size() + " OperatorContributor(s), " +
                    alignmentHandlers.size() + " AlignmentHandler(s)");

        } catch (ServiceConfigurationError serviceError) {
            System.err.println(serviceError);
            serviceError.printStackTrace();
        }

    }

    /**
     * Discover and instantiate services of the given type via BEASTClassLoader.
     */
    private <T> void discoverServices(Class<T> serviceType, List<T> registry) {
        Map<String, Set<String>> providers = BEASTClassLoader.getServices();
        Set<String> providerNames = providers.get(serviceType.getName());
        if (providerNames == null || providerNames.isEmpty()) return;

        for (String clsStr : providerNames) {
            try {
                Class<?> cls = BEASTClassLoader.forName(clsStr, serviceType.getName());
                Object obj = cls.getDeclaredConstructor().newInstance();
                registry.add(serviceType.cast(obj));
                System.out.println("Discovered " + serviceType.getSimpleName() + ": " + clsStr);
            } catch (Exception e) {
                // skip if cannot instantiate
            }
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

    private void registerGeneratorToBEAST(final List<Class<? extends GeneratorToBEAST>> generatorToBEASTs,
                                          String extensionName) {
        for (Class<? extends GeneratorToBEAST> c : generatorToBEASTs) {
            try {
                // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
                GeneratorToBEAST<?,?> generatorToBEAST = (GeneratorToBEAST<?,?>) c.getDeclaredConstructor().newInstance();
                if (this.generatorToBEASTMap.containsKey(generatorToBEAST))
                    LoggerUtils.log.warning(generatorToBEAST + " exists in register, overwrite previous one !");
                Class<?> genClass = generatorToBEAST.getGeneratorClass();
                this.generatorToBEASTMap.put(genClass, generatorToBEAST);
                this.generatorSources.put(genClass, extensionName);
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

    /**
     * @return the extension that registered a GeneratorToBEAST for the given generator class,
     *         or null if no mapping is loaded for that generator.
     */
    public String getGeneratorSource(Class<?> generatorClass) {
        return generatorSources != null ? generatorSources.get(generatorClass) : null;
    }

    /**
     * @return names of all loaded LPhyBEASTMapping extension classes.
     */
    public List<String> getLoadedExtensions() {
        return loadedExtensions != null ? loadedExtensions : List.of();
    }

    /**
     * Build a summary of which generators are provided by each loaded extension.
     */
    public Map<String, List<String>> getExtensionGeneratorSummary() {
        Map<String, List<String>> summary = new LinkedHashMap<>();
        if (generatorSources == null) return summary;
        for (var entry : generatorSources.entrySet()) {
            summary.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey().getSimpleName());
        }
        return summary;
    }

}
