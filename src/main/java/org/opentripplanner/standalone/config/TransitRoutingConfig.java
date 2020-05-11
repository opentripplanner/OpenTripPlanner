package org.opentripplanner.standalone.config;

import org.opentripplanner.model.TransferPriority;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitTuningParameters;
import org.opentripplanner.transit.raptor.api.request.DynamicSearchWindowCoefficients;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;

import java.util.Map;

/**
 * @see RaptorTuningParameters for documentaion of tuning parameters.
 */
public final class TransitRoutingConfig
    implements
        RaptorTuningParameters,
        TransitTuningParameters
{

    private final int maxNumberOfTransfers;
    private final int scheduledTripBinarySearchThreshold;
    private final int iterationDepartureStepInSeconds;
    private final int searchThreadPoolSize;
    private final Map<TransferPriority, Integer> stopTransferCost;
    private final DynamicSearchWindowCoefficients dynamicSearchWindowCoefficients;

    public TransitRoutingConfig(NodeAdapter c) {
        RaptorTuningParameters dft = new RaptorTuningParameters() {};

        this.maxNumberOfTransfers = c.asInt(
            "maxNumberOfTransfers",
            dft.maxNumberOfTransfers()
        );
        this.scheduledTripBinarySearchThreshold = c.asInt(
            "scheduledTripBinarySearchThreshold",
            dft.scheduledTripBinarySearchThreshold()
        );
        this.iterationDepartureStepInSeconds = c.asInt(
            "iterationDepartureStepInSeconds",
            dft.iterationDepartureStepInSeconds()
        );
        this.searchThreadPoolSize = c.asInt(
            "searchThreadPoolSize",
            dft.searchThreadPoolSize()
        );
        // Dynamic Search Window
        this.dynamicSearchWindowCoefficients = new DynamicSearchWindowConfig(
            c.path("dynamicSearchWindow")
        );
        this.stopTransferCost = c.asEnumMapAllKeysRequired(
            "stopTransferCost",
            TransferPriority.class,
            NodeAdapter::asInt
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
        return dynamicSearchWindowCoefficients;
    }

    @Override
    public boolean enableStopTransferPriority() {
        return stopTransferCost != null;
    }

    @Override
    public Integer stopTransferCost(TransferPriority key) {
        return stopTransferCost.get(key);
    }

    private static class DynamicSearchWindowConfig
            implements DynamicSearchWindowCoefficients
    {
        private final double minTripTimeCoefficient;
        private final int minWinTimeMinutes;
        private final int maxWinTimeMinutes;
        private final int stepMinutes;

        public DynamicSearchWindowConfig(NodeAdapter dsWin) {
            DynamicSearchWindowCoefficients dsWinDft = new DynamicSearchWindowCoefficients() {};
            this.minTripTimeCoefficient = dsWin.asDouble("minTripTimeCoefficient", dsWinDft.minTripTimeCoefficient());
            this.minWinTimeMinutes = dsWin.asInt("minWinTimeMinutes",  dsWinDft.minWinTimeMinutes());
            this.maxWinTimeMinutes = dsWin.asInt("maxWinTimeMinutes",  dsWinDft.maxWinTimeMinutes());
            this.stepMinutes = dsWin.asInt("stepMinutes",  dsWinDft.stepMinutes());
        }

        @Override public double minTripTimeCoefficient() { return minTripTimeCoefficient; }
        @Override public int minWinTimeMinutes() { return minWinTimeMinutes; }
        @Override public int maxWinTimeMinutes() { return maxWinTimeMinutes; }
        @Override public int stepMinutes() { return stepMinutes; }
    }
}
