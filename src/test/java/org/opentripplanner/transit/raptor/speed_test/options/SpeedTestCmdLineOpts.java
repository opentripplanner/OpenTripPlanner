package org.opentripplanner.transit.raptor.speed_test.options;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpeedTestCmdLineOpts {

    private static final String HELP = "h";
    private static final String VERBOSE = "v";
    private static final String ROOT_DIR = "d";
    private static final String PROFILES = "p";
    private static final String TEST_CASES = "c";
    private static final String NUM_OF_ITINERARIES = "i";
    private static final String SAMPLE_TEST_N_TIMES = "n";
    private static final String NUM_OF_ADD_TRANSFERS = "t";
    private static final String COMPARE_HEURISTICS = "q";
    private static final String SKIP_COST = "0";
    private static final String DEBUG = "D";
    private static final String DEBUG_REQUEST = "R";
    private static final String DEBUG_STOPS = "S";
    private static final String DEBUG_PATH = "P";
    private static final boolean OPTION_UNKNOWN_THEN_FAIL = false;

    private CommandLine cmd;

    // For list of options see super class

    public SpeedTestCmdLineOpts(String[] args) {
        Options options = options();

        CommandLineParser cmdParser = new DefaultParser();

        try {
            this.cmd = cmdParser.parse(options, args, OPTION_UNKNOWN_THEN_FAIL);

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
            if (stops.get(i).startsWith("*")) { return i; }
        }
        return 0;
    }

    public boolean verbose() {
        return cmd.hasOption(VERBOSE);
    }

    public int numOfItineraries() {
        return Integer.parseInt(cmd.getOptionValue(NUM_OF_ITINERARIES, "3"));
    }

    public int numberOfTestsSamplesToRun() {
        int dftLen = profiles().length - (compareHeuristics() ? 1 : 0);
        String dftLenAsStr = Integer.toString(dftLen);
        return Integer.parseInt(cmd.getOptionValue(SAMPLE_TEST_N_TIMES, dftLenAsStr));
    }

    public int numOfExtraTransfers() {
        return Integer.parseInt(cmd.getOptionValue(NUM_OF_ADD_TRANSFERS, "5"));
    }

    public SpeedTestProfile[] profiles() {
        return cmd.hasOption(PROFILES) ? SpeedTestProfile.parse(cmd.getOptionValue(PROFILES)) : SpeedTestProfile.values();
    }

    public boolean compareHeuristics() {
        return cmd.hasOption(COMPARE_HEURISTICS);
    }

    public boolean skipCost() {
        return cmd.hasOption(SKIP_COST);
    }

    public List<String> testCaseIds() {
        return parseCSVList(TEST_CASES);
    }

    private Options options() {
        Options options = new Options();

        // General options
        options.addOption(HELP, "help", false, "Print all command line options, then exit. (Optional)");
        options.addOption(VERBOSE, "verbose", false, "Verbose output, print itineraries.");
        options.addOption(ROOT_DIR, "dir", true, "The directory where network and input files are located. (Optional)");

        // Search options
        options.addOption(PROFILES, "profiles", true, "A coma separated list of configuration profiles:\n" + String.join("\n", SpeedTestProfile.options()));
        options.addOption(TEST_CASES, "testCases", true, "A coma separated list of test case ids to run.");
        options.addOption(SAMPLE_TEST_N_TIMES, "sampleTestNTimes", true, "Repeat the test N times. Profiles are altered in a round robin fashion.");
        options.addOption(NUM_OF_ADD_TRANSFERS, "nExtraTransfers", true, "The maximum number of extra transfers allowed relative to the path with the fewest transfers.");

        // Result options
        options.addOption(NUM_OF_ITINERARIES, "numOfItineraries", true, "Number of itineraries to return.");
        options.addOption(COMPARE_HEURISTICS, "compare", false, "Compare heuristics for the listed profiles. The 1st profile is compared with 2..n listed profiles.");

        options.addOption(SKIP_COST, "skipCost", false, "Skip cost when comparing results.");
        // Debug options
        options.addOption(DEBUG, "debug", false, "Enable debug info.");
        options.addOption(DEBUG_STOPS, "debugStops", true, "A coma separated list of stops to debug.");
        options.addOption(DEBUG_PATH, "debugPath", true, "A coma separated list of stops representing a trip/path to debug. " +
                "Use a '*' to indicate where to start debugging. For example '1,*2,3' will print event at stop 2 and 3, " +
                "but not stop 1 for all trips starting with the given stop sequence.");
        options.addOption(DEBUG_REQUEST, "debugRequest", false, "Debug request.");
        return options;
   }

    List<String> parseCSVList(String opt) {
        return cmd.hasOption(opt)
                ? Arrays.asList(cmd.getOptionValue(opt).split("\\s*,\\s*"))
                : Collections.emptyList();
    }

    private List<Integer> parseCSVToInt(String opt) {
        return cmd.hasOption(opt)
                ? parseCSVList(opt).stream().map(Integer::valueOf).collect(Collectors.toList())
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
