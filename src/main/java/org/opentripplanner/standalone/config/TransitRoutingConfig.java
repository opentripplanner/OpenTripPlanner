package org.opentripplanner.standalone.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.StopTransferPriority;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.transit.raptor.api.request.DynamicSearchWindowCoefficients;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;

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
    private final int transferCacheMaxSize;
    private final List<Duration> pagingSearchWindowAdjustments;

    private final Map<StopTransferPriority, Integer> stopTransferCost;
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
        this.stopTransferCost = c.asEnumMapAllKeysRequired(
            "stopTransferCost",
            StopTransferPriority.class,
            NodeAdapter::asInt
        );
        this.transferCacheMaxSize = c.asInt(
            "transferCacheMaxSize",
            25
        );

        this.pagingSearchWindowAdjustments = c.asDurations(
            "pagingSearchWindowAdjustments", PAGING_SEARCH_WINDOW_ADJUSTMENTS
        );

        this.dynamicSearchWindowCoefficients = new DynamicSearchWindowConfig(
            c.path("dynamicSearchWindow")
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
    public Integer stopTransferCost(StopTransferPriority key) {
        return stopTransferCost.get(key);
    }

    @Override
    public int transferCacheMaxSize() {
        return transferCacheMaxSize;
    }

    @Override
    public List<Duration> pagingSearchWindowAdjustments() {
        return pagingSearchWindowAdjustments;
    }


    private static class DynamicSearchWindowConfig
            implements DynamicSearchWindowCoefficients
    {
        private final double minTransitTimeCoefficient;
        private final double minWaitTimeCoefficient;
        private final int minWinTimeMinutes;
        private final int maxWinTimeMinutes;
        private final int stepMinutes;

        public DynamicSearchWindowConfig(NodeAdapter dsWin) {
            DynamicSearchWindowCoefficients dsWinDft = new DynamicSearchWindowCoefficients() {};
            this.minTransitTimeCoefficient = dsWin.asDouble("minTransitTimeCoefficient", dsWinDft.minTransitTimeCoefficient());
            this.minWaitTimeCoefficient = dsWin.asDouble("minWaitTimeCoefficient", dsWinDft.minWaitTimeCoefficient());
            this.minWinTimeMinutes = dsWin.asInt("minWinTimeMinutes",  dsWinDft.minWinTimeMinutes());
            this.maxWinTimeMinutes = dsWin.asInt("maxWinTimeMinutes",  dsWinDft.maxWinTimeMinutes());
            this.stepMinutes = dsWin.asInt("stepMinutes",  dsWinDft.stepMinutes());
        }

        @Override public double minTransitTimeCoefficient() { return minTransitTimeCoefficient; }
        @Override public double minWaitTimeCoefficient() { return minWaitTimeCoefficient; }
        @Override public int minWinTimeMinutes() { return minWinTimeMinutes; }
        @Override public int maxWinTimeMinutes() { return maxWinTimeMinutes; }
        @Override public int stepMinutes() { return stepMinutes; }
    }
}
