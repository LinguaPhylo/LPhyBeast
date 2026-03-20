package lphybeast;

import beast.base.core.ProgramStatus;
import beast.base.inference.Logger;
import beast.base.parser.XMLParser;
import beast.base.util.Randomizer;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.Utils6;
import lphy.core.logger.LoggerUtils;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Top-level CLI for LPhyBEAST with subcommands for converting LPhy scripts,
 * running BEAST MCMC, and managing packages.
 */
@Command(name = "lphybeast",
        description = "LPhyBEAST: convert LPhy scripts to BEAST XML and manage packages.",
        version = {"LPhyBEAST " + LPhyBeastCMD.VERSION,
                "Local JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                "OS: ${os.name} ${os.version} ${os.arch}"},
        mixinStandardHelpOptions = true,
        subcommands = {
                LPhyBeastMain.ConvertCmd.class,
                LPhyBeastMain.RunCmd.class,
                LPhyBeastMain.InstallCmd.class,
                LPhyBeastMain.ListCmd.class,
                LPhyBeastMain.RemoveCmd.class,
                HelpCommand.class
        })
public class LPhyBeastMain implements Runnable {

    @Option(names = {"--packagedir"},
            description = "Override the LPhyBEAST package directory (default: platform-specific).",
            scope = ScopeType.INHERIT)
    String packageDir;

    /**
     * Set up the package directory before any PackageManager call.
     * Uses Utils6.getPackageUserDir("LPhyBEAST") by default,
     * overridden by --packagedir if provided.
     */
    static void initPackageDir(String overrideDir) {
        String pkgDir;
        if (overrideDir != null && !overrideDir.isBlank()) {
            pkgDir = overrideDir;
        } else {
            pkgDir = Utils6.getPackageUserDir("LPhyBEAST");
        }
        // Redirect PackageManager to our directory
        System.setProperty("beast.user.package.dir", pkgDir);
    }

    @Override
    public void run() {
        // No subcommand given — print help
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        // must set -Dpicocli.disable.closures=true using picocli:4.7.0+
        // otherwise java.lang.NoClassDefFoundError: groovy.lang.Closure
        int exitCode = new CommandLine(new LPhyBeastMain())
                .setExecutionStrategy(new RunLast())
                .execute(args);

        if (exitCode != 0)
            LoggerUtils.log.severe("LPhyBEAST does not exit normally!");
        System.exit(exitCode);
    }

    // ──────────────────────────────────────────────
    // Subcommand: convert
    // ──────────────────────────────────────────────

    @Command(name = "convert",
            description = "Convert an LPhy script to BEAST XML.",
            mixinStandardHelpOptions = true)
    static class ConvertCmd implements Callable<Integer> {

        @ParentCommand
        LPhyBeastMain parent;

        @Parameters(paramLabel = "LPhy_script",
                description = "LPhy model specification file.")
        Path infile;

        @Option(names = {"-o", "--out"},
                description = "Output BEAST XML file path.")
        Path outfile;

        @Option(names = {"-wd", "--workdir"},
                description = "Set working directory for relative paths.")
        Path wd;

        @Option(names = {"-l", "--chainLength"}, defaultValue = "1000000",
                description = "MCMC chain length (default: 1000000).")
        long chainLength;

        @Option(names = {"-b", "--preBurnin"}, defaultValue = "-1",
                description = "Pre-burnin samples. If < 0, estimated automatically.")
        int preBurnin;

        @Option(names = {"-le", "--logEvery"},
                description = "State logging frequency.")
        long logEvery;

        @Option(names = {"-sp", "--sampleFromPrior"}, defaultValue = "false",
                description = "Sample from prior.")
        boolean sampleFromPrior;

        @Option(names = {"-r", "--replicates"}, defaultValue = "1",
                description = "Number of replicate XMLs.")
        int repTot;

        @Option(names = {"-D", "--data"}, split = ";",
                description = "Replace constants in LPhy script (e.g. -D \"n=12;L=100\").")
        String[] lphyConst = null;

        @Option(names = {"-No", "--notlog"}, split = ";",
                description = "Variables to exclude from logging.")
        String[] varNotLog = null;

        @Option(names = {"-cca", "--compressConstantAlignments"},
                description = "Compress constant-site alignments (0=off, 1=all states, 2=ignore gaps).")
        int compressConstantAlignment;

        @Option(names = {"-seed"},
                description = "Seed for the LPhy script.")
        int seed;

        @Option(names = {"-u", "--logunicode"}, defaultValue = "false",
                description = "Log IDs in unicode.")
        boolean logunicode;

        @Option(names = {"-vf", "--version_file"}, split = ",",
                description = "BEAST version.xml file(s) for service registration.")
        String[] versionFiles = null;

        @Option(names = {"-m2", "--model2"},
                description = "Second LPhy script for model misspecification test.")
        Path model2File = null;

        @Option(names = {"-lgX", "--logOriginalXmls"}, defaultValue = "false",
                description = "Log both original XMLs for model misspecification.")
        boolean log_orignal_xmls;

        @Option(names = {"-MC3", "--mc3"}, defaultValue = "false",
                description = "Use Metropolis Coupled MCMC.")
        boolean useMC3;

        @Option(names = {"--chains"}, defaultValue = "4",
                description = "Number of MC3 chains.")
        int chains;

        @Option(names = {"--deltaTemperature"}, defaultValue = "0.15",
                description = "MC3 delta temperature.")
        double deltaTemperature;

        @Option(names = {"--resampleEvery"}, defaultValue = "1000",
                description = "MC3 resample frequency.")
        int resampleEvery;

        @Option(names = {"--target"}, defaultValue = "0.234",
                description = "MC3 target acceptance rate.")
        double target;

        @Option(names = {"-ob", "--observedParam"}, split = ";",
                description = "Variables to mark as observed.")
        String[] observedParam;

        @Option(names = {"-rs", "--randomStart"},
                description = "Do not initialize MCMC from true values.")
        boolean randomStart;

        @Option(names = {"--operatorSchedule"},
                description = "Operator schedule (e.g. 'targeted').")
        String operatorSchedule = null;

        @Option(names = {"-t", "--startingTree"},
                description = "Newick file for starting tree.")
        Path startingTreeFile = null;

        /**
         * Run the conversion. Returns the output XML path for use by RunCmd.
         */
        Path doConvert() throws Exception {
            initPackageDir(parent != null ? parent.packageDir : null);

            LPhyBEASTLoader.addBEAST2Services(versionFiles);
            LPhyBEASTLoader loader = LPhyBEASTLoader.getInstance();

            LPhyBeastConfig config = new LPhyBeastConfig(infile, outfile, wd,
                    lphyConst, varNotLog, logunicode, randomStart);
            config.setMCMCConfig(chainLength, preBurnin, logEvery, sampleFromPrior);
            config.setCompressConstantAlignment(compressConstantAlignment);
            config.setModelMisspec(model2File, log_orignal_xmls);
            config.setMC3Config(useMC3, chains, deltaTemperature, resampleEvery, target);
            config.setObservedParamID(observedParam);
            config.setOperatorSchedule(operatorSchedule);
            config.setStartingTreeFile(startingTreeFile);

            if (seed > 0)
                lphy.core.simulator.RandomUtils.setSeed(seed);

            LPhyBeast lphyBeast = new LPhyBeast(loader, config, repTot);
            lphyBeast.run();

            // Return the output file path for RunCmd
            return config.outPath;
        }

        @Override
        public Integer call() {
            try {
                doConvert();
            } catch (java.io.FileNotFoundException e) {
                throw new PicocliException("Failed to read LPhy script: " + infile, e);
            } catch (UnsupportedOperationException e) {
                throw new PicocliException("\nMissing LPhyBEAST mapping:\n  " + e.getMessage(), e);
            } catch (Exception e) {
                e.printStackTrace();
                throw new PicocliException(e.toString());
            }
            return 0;
        }
    }

    // ──────────────────────────────────────────────
    // Subcommand: run
    // ──────────────────────────────────────────────

    @Command(name = "run",
            description = "Convert an LPhy script to BEAST XML, then run BEAST MCMC.",
            mixinStandardHelpOptions = true)
    static class RunCmd implements Callable<Integer> {

        @ParentCommand
        LPhyBeastMain parent;

        // Embed all convert options via mixin
        @Mixin
        ConvertCmd convertCmd = new ConvertCmd();

        @Option(names = {"--beast-seed"}, defaultValue = "127",
                description = "Random seed for BEAST MCMC (default: 127).")
        long beastSeed;

        @Option(names = {"--threads"}, defaultValue = "1",
                description = "Number of threads for BEAST (default: 1).")
        int threads;

        @Option(names = {"--resume"}, defaultValue = "false",
                description = "Resume from previous state file.")
        boolean resume;

        @Option(names = {"--overwrite"}, defaultValue = "false",
                description = "Overwrite existing log files.")
        boolean overwrite;

        @Override
        public Integer call() {
            try {
                // Pass parent down to the mixin
                convertCmd.parent = parent;
                Path xmlPath = convertCmd.doConvert();

                if (xmlPath == null || !Files.exists(xmlPath)) {
                    System.err.println("XML file not produced: " + xmlPath);
                    return 1;
                }

                System.out.println("Running BEAST on " + xmlPath);

                // Configure BEAST runtime directly — avoids BeastMCMC.parseArgs()
                // which calls PackageManager.loadExternalJars() internally
                // (problematic when running on the classpath instead of module path)
                if (resume) {
                    Logger.FILE_MODE = Logger.LogFileMode.resume;
                    System.setProperty("beast.resume", "true");
                } else if (overwrite) {
                    Logger.FILE_MODE = Logger.LogFileMode.overwrite;
                }

                ProgramStatus.m_nThreads = threads;
                ProgramStatus.g_exec = Executors.newFixedThreadPool(threads);
                Randomizer.setSeed(beastSeed);

                File xmlFile = xmlPath.toAbsolutePath().toFile();
                beast.base.inference.Runnable runnable = new XMLParser().parseFile(xmlFile);
                runnable.setStateFile(xmlFile.getName() + ".state", resume);
                runnable.run();

                ProgramStatus.g_exec.shutdown();
                ProgramStatus.g_exec.shutdownNow();

            } catch (Exception e) {
                e.printStackTrace();
                throw new PicocliException(e.toString());
            }
            return 0;
        }
    }

    // ──────────────────────────────────────────────
    // Subcommand: install
    // ──────────────────────────────────────────────

    @Command(name = "install",
            description = "Install a Maven package into LPhyBEAST's package cache.",
            mixinStandardHelpOptions = true)
    static class InstallCmd implements Callable<Integer> {

        @ParentCommand
        LPhyBeastMain parent;

        @Parameters(paramLabel = "groupId:artifactId:version",
                description = "Maven coordinate of the package to install.")
        String coordinate;

        @Override
        public Integer call() {
            try {
                initPackageDir(parent != null ? parent.packageDir : null);

                String[] parts = coordinate.split(":");
                if (parts.length != 3) {
                    System.err.println("Expected format: groupId:artifactId:version");
                    return 1;
                }

                String pkgDir = System.getProperty("beast.user.package.dir");
                Files.createDirectories(Path.of(pkgDir));

                PackageManager.loadExternalJars();

                System.out.println("Installing " + coordinate + " into " + pkgDir);
                PackageManager.installMavenPackage(parts[0], parts[1], parts[2]);
                System.out.println("Done.");

            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
            return 0;
        }
    }

    // ──────────────────────────────────────────────
    // Subcommand: list
    // ──────────────────────────────────────────────

    @Command(name = "list",
            description = "List installed packages.",
            mixinStandardHelpOptions = true)
    static class ListCmd implements Callable<Integer> {

        @ParentCommand
        LPhyBeastMain parent;

        @Option(names = {"--available"}, defaultValue = "false",
                description = "Also show available (remote) packages.")
        boolean showAvailable;

        @Override
        public Integer call() {
            try {
                initPackageDir(parent != null ? parent.packageDir : null);

                PackageManager.initialise();

                Map<String, beast.pkgmgmt.Package> packageMap = new TreeMap<>();
                PackageManager.addInstalledPackages(packageMap);

                if (packageMap.isEmpty()) {
                    System.out.println("No installed packages.");
                } else {
                    System.out.println("Installed packages:");
                    System.out.printf("  %-30s %s%n", "NAME", "VERSION");
                    for (var entry : packageMap.entrySet()) {
                        var pkg = entry.getValue();
                        String ver = pkg.getInstalledVersion() != null
                                ? pkg.getInstalledVersion().toString() : "?";
                        System.out.printf("  %-30s %s%n", entry.getKey(), ver);
                    }
                }

                if (showAvailable) {
                    Map<String, beast.pkgmgmt.Package> availMap = new TreeMap<>();
                    PackageManager.addAvailablePackages(availMap);
                    System.out.println("\nAvailable packages:");
                    System.out.printf("  %-30s %s%n", "NAME", "LATEST VERSION");
                    for (var entry : availMap.entrySet()) {
                        var pkg = entry.getValue();
                        String ver = pkg.getLatestVersion() != null
                                ? pkg.getLatestVersion().toString() : "?";
                        System.out.printf("  %-30s %s%n", entry.getKey(), ver);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
            return 0;
        }
    }

    // ──────────────────────────────────────────────
    // Subcommand: remove
    // ──────────────────────────────────────────────

    @Command(name = "remove",
            description = "Remove an installed Maven package.",
            mixinStandardHelpOptions = true)
    static class RemoveCmd implements Callable<Integer> {

        @ParentCommand
        LPhyBeastMain parent;

        @Parameters(paramLabel = "groupId:artifactId",
                description = "Maven coordinate of the package to remove.")
        String coordinate;

        @Override
        public Integer call() {
            try {
                initPackageDir(parent != null ? parent.packageDir : null);

                String[] parts = coordinate.split(":");
                if (parts.length != 2) {
                    System.err.println("Expected format: groupId:artifactId");
                    return 1;
                }

                PackageManager.initialise();

                System.out.println("Removing " + coordinate);
                PackageManager.uninstallMavenPackage(parts[0], parts[1]);
                System.out.println("Done.");

            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
            return 0;
        }
    }
}
