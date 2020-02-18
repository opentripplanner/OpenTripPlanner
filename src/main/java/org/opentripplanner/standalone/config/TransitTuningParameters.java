package org.opentripplanner.standalone.config;

import org.opentripplanner.transit.raptor.api.request.DynamicSearchWindowCoefficients;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;

/**
 * @see RaptorTuningParameters for documentaion of tuning parameters.
 */
public final class TransitTuningParameters implements RaptorTuningParameters {

    private final int maxNumberOfTransfers;
    private final int scheduledTripBinarySearchThreshold;
    private final int iterationDepartureStepInSeconds;
    private final int searchThreadPoolSize;
    private final double dsWinMinTripTimeCoefficient;
    private final int dsWinMinTimeMinutes;
    private final int dsWinStepMinutes;

    public TransitTuningParameters(NodeAdapter c) {
        RaptorTuningParameters dft = new RaptorTuningParameters() {};

        this.maxNumberOfTransfers = c.asInt("maxNumberOfTransfers", dft.maxNumberOfTransfers());
        this.scheduledTripBinarySearchThreshold = c.asInt(
                "scheduledTripBinarySearchThreshold", dft.scheduledTripBinarySearchThreshold()
        );
        this.iterationDepartureStepInSeconds = c.asInt(
                "iterationDepartureStepInSeconds", dft.iterationDepartureStepInSeconds()
        );
        this.searchThreadPoolSize = c.asInt(
                "searchThreadPoolSize", dft.searchThreadPoolSize()
        );

        // Dynamic Search Window
        NodeAdapter dsWin = c.path("dynamicSearchWindow");
        DynamicSearchWindowCoefficients dsWinDft = dft.dynamicSearchWindowCoefficients();
        this.dsWinMinTripTimeCoefficient = dsWin.asDouble(
                "minTripTimeCoefficient", dsWinDft.minTripTimeCoefficient()
        );
        this.dsWinMinTimeMinutes = dsWin.asInt(
                "minTimeMinutes",  dsWinDft.minTimeMinutes()
        );
        this.dsWinStepMinutes = dsWin.asInt(
                "stepMinutes",  dsWinDft.stepMinutes()
        );
    }

    @Override
    public int maxNumberOfTransfers() {
        return maxNumberOfTransfers;
    }

    @Override
    public int scheduledTripBinarySearchThreshold() {
        return scheduledTripBinarySearchThreshold;
    }

    @Override
    public int iterationDepartureStepInSeconds() {
        return iterationDepartureStepInSeconds;
    }

    @Override
    public int searchThreadPoolSize() {
        return searchThreadPoolSize;
    }

    @Override
    public DynamicSearchWindowCoefficients dynamicSearchWindowCoefficients() {
        return new DynamicSearchWindowCoefficients() {
            @Override public double minTripTimeCoefficient() { return dsWinMinTripTimeCoefficient; }
            @Override public int minTimeMinutes() { return dsWinMinTimeMinutes; }
            @Override public int stepMinutes() { return dsWinStepMinutes; }
        };
    }
}
