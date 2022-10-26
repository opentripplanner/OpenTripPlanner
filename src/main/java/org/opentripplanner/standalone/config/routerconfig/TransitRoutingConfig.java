package org.opentripplanner.standalone.config.routerconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.raptor.api.request.DynamicSearchWindowCoefficients;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;

/**
 * @see RaptorTuningParameters for documentaion of tuning parameters.
 */
public final class TransitRoutingConfig implements RaptorTuningParameters, TransitTuningParameters {

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

    this.maxNumberOfTransfers =
      c.of("maxNumberOfTransfers").since(NA).summary("TODO").asInt(dft.maxNumberOfTransfers());
    this.scheduledTripBinarySearchThreshold =
      c
        .of("scheduledTripBinarySearchThreshold")
        .since(NA)
        .summary("TODO")
        .asInt(dft.scheduledTripBinarySearchThreshold());
    this.iterationDepartureStepInSeconds =
      c
        .of("iterationDepartureStepInSeconds")
        .since(NA)
        .summary("TODO")
        .asInt(dft.iterationDepartureStepInSeconds());
    this.searchThreadPoolSize =
      c.of("searchThreadPoolSize").since(NA).summary("TODO").asInt(dft.searchThreadPoolSize());
    // Dynamic Search Window
    this.stopTransferCost =
      c
        .of("stopTransferCost")
        .since(NA)
        .summary("TODO")
        .asEnumMapAllKeysRequired(StopTransferPriority.class, Integer.class);
    this.transferCacheMaxSize = c.of("transferCacheMaxSize").since(NA).summary("TODO").asInt(25);

    this.pagingSearchWindowAdjustments =
      c
        .of("pagingSearchWindowAdjustments")
        .since(NA)
        .summary("TODO")
        .asDurations(PAGING_SEARCH_WINDOW_ADJUSTMENTS);

    this.dynamicSearchWindowCoefficients =
      new DynamicSearchWindowConfig(
        c
          .of("dynamicSearchWindow")
          .since(NA)
          .summary("TODO")
          .description(/*TODO DOC*/"TODO")
          .asObject()
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

  private static class DynamicSearchWindowConfig implements DynamicSearchWindowCoefficients {

    private final double minTransitTimeCoefficient;
    private final double minWaitTimeCoefficient;
    private final int minWinTimeMinutes;
    private final int maxWinTimeMinutes;
    private final int stepMinutes;

    public DynamicSearchWindowConfig(NodeAdapter dsWin) {
      DynamicSearchWindowCoefficients dsWinDft = new DynamicSearchWindowCoefficients() {};
      this.minTransitTimeCoefficient =
        dsWin
          .of("minTransitTimeCoefficient")
          .since(NA)
          .summary("TODO")
          .asDouble(dsWinDft.minTransitTimeCoefficient());
      this.minWaitTimeCoefficient =
        dsWin
          .of("minWaitTimeCoefficient")
          .since(NA)
          .summary("TODO")
          .asDouble(dsWinDft.minWaitTimeCoefficient());
      this.minWinTimeMinutes =
        dsWin.of("minWinTimeMinutes").since(NA).summary("TODO").asInt(dsWinDft.minWinTimeMinutes());
      this.maxWinTimeMinutes =
        dsWin.of("maxWinTimeMinutes").since(NA).summary("TODO").asInt(dsWinDft.maxWinTimeMinutes());
      this.stepMinutes =
        dsWin.of("stepMinutes").since(NA).summary("TODO").asInt(dsWinDft.stepMinutes());
    }

    @Override
    public double minTransitTimeCoefficient() {
      return minTransitTimeCoefficient;
    }

    @Override
    public double minWaitTimeCoefficient() {
      return minWaitTimeCoefficient;
    }

    @Override
    public int minWinTimeMinutes() {
      return minWinTimeMinutes;
    }

    @Override
    public int maxWinTimeMinutes() {
      return maxWinTimeMinutes;
    }

    @Override
    public int stepMinutes() {
      return stepMinutes;
    }
  }
}
