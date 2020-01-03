package org.opentripplanner.transit.raptor.speed_test.cli;

import org.apache.commons.cli.Options;
import org.opentripplanner.transit.raptor.speed_test.SpeedTestProfile;

import java.util.List;

public class SpeedTestCmdLineOpts extends CommandLineOpts {

    // For list of options see super class

    public SpeedTestCmdLineOpts(String[] args) {
        super(args);
    }

    @Override
    Options speedTestOptions() {
        Options options = super.speedTestOptions();
        options.addOption(NUM_OF_ITINERARIES, "numOfItineraries", true, "Number of itineraries to return.");
        options.addOption(VERBOSE, "verbose", false, "Verbose output, print itineraries.");
        options.addOption(SAMPLE_TEST_N_TIMES, "sampleTestNTimes", true, "Repeat the test N times. Profiles are altered in a round robin fashion.");
        options.addOption(PROFILES, "profiles", true, "A coma separated list of configuration profiles:\n" + String.join("\n", SpeedTestProfile.options()));
        options.addOption(COMPARE_HEURISTICS, "compare", false, "Compare heuristics for the listed profiles. Must be 2 profiles.");
        options.addOption(TEST_CASES, "testCases", true, "A coma separated list of test case numbers to run.");
        options.addOption(NUM_OF_ADD_TRANSFERS, "nExtraTransfers", true, "The maximum number of extra transfers allowed relative to the path with the fewest transfers.");
        return options;
    }

   public boolean verbose() {
        return cmd.hasOption(VERBOSE);
    }

    public int numOfItineraries() {
        return Integer.valueOf(cmd.getOptionValue(NUM_OF_ITINERARIES, "3"));
    }

    public int numberOfTestsSamplesToRun() {
        int dftLen = compareHeuristics() ? profiles().length - 1 : profiles().length;
        return Integer.valueOf(cmd.getOptionValue(SAMPLE_TEST_N_TIMES, Integer.toString(dftLen)));
    }

    public int numOfExtraTransfers() {
        return Integer.valueOf(cmd.getOptionValue(NUM_OF_ADD_TRANSFERS, "5"));
    }

    public SpeedTestProfile[] profiles() {
        return cmd.hasOption(PROFILES) ? SpeedTestProfile.parse(cmd.getOptionValue(PROFILES)) : SpeedTestProfile.values();
    }

    public boolean compareHeuristics() {
        return cmd.hasOption(COMPARE_HEURISTICS);
    }

    public List<String> testCases() {
        return parseCSVList(TEST_CASES);
    }
}
