package org.opentripplanner.transit.speed_test.options;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;

public class SpeedTestCmdLineOpts {

  private static final String HELP = "h";
  private static final String VERBOSE = "v";
  private static final String ROOT_DIR = "d";
  private static final String PROFILES = "p";
  private static final String TEST_CASES = "c";
  private static final String CATEGORIES = "t";
  private static final String GROUP_RESULTS_BY_CATEGORY = "T";
  private static final String NUM_OF_ITINERARIES = "i";
  private static final String SAMPLE_TEST_N_TIMES = "n";
  private static final String SKIP_COST = "0";
  private static final String DEBUG_STOPS = "S";
  private static final String DEBUG_PATH = "P";
  private static final boolean OPTION_UNKNOWN_THEN_FAIL = false;

  private CommandLine cmd;

  // For list of options see super class

  public SpeedTestCmdLineOpts(String[] args) {
    Options options = options();

    try {
      this.cmd = new DefaultParser().parse(options, args, OPTION_UNKNOWN_THEN_FAIL);

      if (printHelpOptSet()) {
        printHelp(options);
        System.exit(0);
      }
      if (!cmd.getArgList().isEmpty()) {
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
    if (!rootDir.exists()) {
      throw new IllegalArgumentException(
        "Unable to find root directory: " + rootDir.getAbsolutePath()
      );
    }
    return rootDir;
  }

  public String debugStops() {
    return cmd.hasOption(DEBUG_STOPS) ? cmd.getOptionValue(DEBUG_STOPS) : null;
  }

  public String debugPath() {
    return cmd.hasOption(DEBUG_PATH) ? cmd.getOptionValue(DEBUG_PATH) : null;
  }

  public boolean verbose() {
    return cmd.hasOption(VERBOSE);
  }

  public int numOfItineraries() {
    return Integer.parseInt(cmd.getOptionValue(NUM_OF_ITINERARIES, "50"));
  }

  public int numberOfTestsSamplesToRun() {
    String defaultValue = Integer.toString(profiles().length);
    return Integer.parseInt(cmd.getOptionValue(SAMPLE_TEST_N_TIMES, defaultValue));
  }

  public SpeedTestProfile[] profiles() {
    return cmd.hasOption(PROFILES)
      ? SpeedTestProfile.parse(cmd.getOptionValue(PROFILES))
      : SpeedTestProfile.values();
  }

  public boolean skipCost() {
    return cmd.hasOption(SKIP_COST);
  }

  public List<String> testCaseIds() {
    return parseCSVList(TEST_CASES);
  }

  public List<String> includeCategories() {
    return parseCSVList(CATEGORIES);
  }

  public boolean groupResultsByCategory() {
    return cmd.hasOption(GROUP_RESULTS_BY_CATEGORY);
  }

  List<String> parseCSVList(String opt) {
    return cmd.hasOption(opt)
      ? Arrays.asList(cmd.getOptionValue(opt).split("\\s*,\\s*"))
      : Collections.emptyList();
  }

  private Options options() {
    Options options = new Options();

    // General options
    options.addOption(HELP, "help", false, "Print all command line options, then exit. (Optional)");
    options.addOption(VERBOSE, "verbose", false, "Verbose output, print itineraries.");
    options.addOption(
      ROOT_DIR,
      "dir",
      true,
      "The directory where graph and input files are located. (Optional)"
    );

    // Search options
    options.addOption(
      PROFILES,
      "profiles",
      true,
      "A coma separated list of configuration profiles:\n" +
      String.join("\n", SpeedTestProfile.options())
    );
    options.addOption(
      TEST_CASES,
      "testCases",
      true,
      "A coma separated list of test case ids to run."
    );
    options.addOption(
      CATEGORIES,
      "categories",
      true,
      "A coma separated list of categories to filter the testcases by."
    );
    options.addOption(
      GROUP_RESULTS_BY_CATEGORY,
      "groupResultsByCategory",
      false,
      "By default the results are aggregated for each metric. Set this flag to print" +
      "metric results for each test-case category."
    );
    options.addOption(
      SAMPLE_TEST_N_TIMES,
      "sampleTestNTimes",
      true,
      "Repeat the test N times. Profiles are altered in a round robin fashion."
    );

    // Result options
    options.addOption(
      NUM_OF_ITINERARIES,
      "numOfItineraries",
      true,
      "Number of itineraries to return."
    );

    options.addOption(SKIP_COST, "skipCost", false, "Skip cost when comparing results.");
    // Debug options
    options.addOption(DEBUG_STOPS, "debugStops", true, "A coma separated list of stops to debug.");
    options.addOption(
      DEBUG_PATH,
      "debugPath",
      true,
      "A coma separated list of stops representing a trip/path to debug. " +
      "Use a '*' to indicate where to start debugging. For example '1,*2,3' will print event at stop 2 and 3, " +
      "but not stop 1 for all trips starting with the given stop sequence."
    );
    return options;
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
