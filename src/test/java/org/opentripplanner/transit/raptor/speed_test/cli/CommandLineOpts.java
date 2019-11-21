package org.opentripplanner.transit.raptor.speed_test.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandLineOpts {
    private static final boolean OPTION_UNKNOWN_THEN_FAIL = false;
    protected CommandLine cmd;

    /* Shared options */
    private static final String ROOT_DIR = "d";
    private static final String HELP = "h";

    /* Speed test options - defined here to keep all option together */
    static final String VERBOSE = "v";
    static final String NUM_OF_ITINERARIES = "i";
    static final String SAMPLE_TEST_N_TIMES = "n";
    static final String PROFILES = "p";
    static final String TEST_CASES = "c";
    static final String NUM_OF_ADD_TRANSFERS = "t";
    static final String COMPARE_HEURISTICS = "q";
    static final String DEBUG = "D";
    static final String DEBUG_REQUEST = "R";
    static final String DEBUG_STOPS = "S";
    static final String DEBUG_PATH = "P";

    public CommandLineOpts(String[] args) {
        Options options = speedTestOptions();

        CommandLineParser cmdParser = new DefaultParser();

        try {
            cmd = cmdParser.parse(options, args, OPTION_UNKNOWN_THEN_FAIL);

            if(printHelpOptSet()) {
                printHelp(options);
                System.exit(0);
            }
            if(!cmd.getArgList().isEmpty()) {
                System.err.println("Unexpected argument(s): " + cmd.getArgList());
                printHelp(options);
                System.exit(-2);
            }

        } catch (ParseException e) {
            System.err.println(e.getMessage());
            printHelp(options);
            System.exit(-1);
        }
    }

    Options speedTestOptions() {
        Options options = new Options();
        options.addOption(ROOT_DIR, "dir", true, "The directory where network and input files are located. (Optional)");
        options.addOption(HELP, "help", false, "Print all command line options, then exit. (Optional)");
        options.addOption(DEBUG_STOPS, "debugStops", true, "A coma separated list of stops to debug.");
        options.addOption(DEBUG_PATH, "debugPath", true, "A coma separated list of stops representing a trip/path to debug. " +
                "Use a '*' to indicate where to start debugging. For example '1,*2,3' will print event at stop 2 and 3, " +
                "but not stop 1 for all trips starting with the given stop sequence.");
        options.addOption(DEBUG_REQUEST, "debugRequest", false, "Debug request.");
        options.addOption(DEBUG, "debug", false, "Enable debug info.");
        return options;
    }

    public File rootDir() {
        File rootDir = new File(cmd.getOptionValue(ROOT_DIR, "."));
        if(!rootDir.exists()) {
            throw new IllegalArgumentException("Unable to find root directory: " + rootDir.getAbsolutePath());
        }
        return rootDir;
    }

    public boolean debug() {
        return cmd.hasOption(DEBUG);
    }

    public boolean debugRequest() {
        return cmd.hasOption(DEBUG_REQUEST);
    }

    public List<Integer> debugStops() {
        return parseCSVToInt(DEBUG_STOPS);
    }

    public List<Integer> debugPath() {
        return parseCSVList(DEBUG_PATH).stream()
                .map(it -> it.startsWith("*") ? it.substring(1) : it)
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    public int debugPathFromStopIndex() {
        List<String> stops = parseCSVList(DEBUG_PATH);
        for (int i = 0; i < stops.size(); ++i) {
            if (stops.get(i).startsWith("*")) return i;
        }
        return 0;
    }


    /* private methods */

    private List<Integer> parseCSVToInt(String opt) {
        return cmd.hasOption(opt)
                ? parseCSVList(opt).stream().map(Integer::valueOf).collect(Collectors.toList())
                : Collections.emptyList();
    }

    List<String> parseCSVList(String opt) {
        return cmd.hasOption(opt)
                ? Arrays.asList(cmd.getOptionValue(opt).split("\\s*,\\s*"))
                : Collections.emptyList();
    }

    private boolean printHelpOptSet() {
        return cmd.hasOption(HELP);
    }

    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(140);
        formatter.printHelp("[options]", options);
    }
}
