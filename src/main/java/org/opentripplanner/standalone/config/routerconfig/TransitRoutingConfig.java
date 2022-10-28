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

  public TransitRoutingConfig(String parameterName, NodeAdapter root) {
    NodeAdapter c = root
      .of(parameterName)
      .since(NA)
      .summary("Configuration for transit searches with RAPTOR.")
      .description(
        """
Some of these parameters for tuning transit routing is only available through configuration and
cannot be set in the routing request. These parameters work together with the default routing
request and the actual routing request.
        """
      )
      .asObject();

    RaptorTuningParameters dft = new RaptorTuningParameters() {};

    this.maxNumberOfTransfers =
      c
        .of("maxNumberOfTransfers")
        .since(NA)
        .summary("This parameter is used to allocate enough memory space for Raptor.")
        .description(
          """
Set it to the maximum number of transfers for any given itinerary expected to be found within the 
entire transit network. The memory overhead of setting this higher than the maximum number of 
transfers is very little so it is better to set it too high then to low.
"""
        )
        .asInt(dft.maxNumberOfTransfers());
    this.scheduledTripBinarySearchThreshold =
      c
        .of("scheduledTripBinarySearchThreshold")
        .since(NA)
        .summary(
          "This threshold is used to determine when to perform a binary trip schedule search."
        )
        .description(
          """
This reduce the number of trips departure time lookups and comparisons. When testing with data from
Entur and all of Norway as a Graph, the optimal value was about 50. If you calculate the departure 
time every time or want to fine tune the performance, changing this may improve the performance a 
few percent.
"""
        )
        .asInt(dft.scheduledTripBinarySearchThreshold());
    this.iterationDepartureStepInSeconds =
      c
        .of("iterationDepartureStepInSeconds")
        .since(NA)
        .summary("Step for departure times between each RangeRaptor iterations.")
        .description(
          """
This is a performance optimization parameter. A transit network usually uses minute resolution for
the timetables, so to match that, set this variable to 60 seconds. Setting it to less than 60 will
not give better result, but degrade performance. Setting it to 120 seconds will improve performance,
but you might get a slack of 60 seconds somewhere in the result.
"""
        )
        .asInt(dft.iterationDepartureStepInSeconds());
    this.searchThreadPoolSize =
      c
        .of("searchThreadPoolSize")
        .since(NA)
        .summary(
          "Split a travel search in smaller jobs and run them in parallel to improve performance."
        )
        .description(
          """
Use this parameter to set the total number of executable threads available across all searches.
Multiple searches can run in parallel - this parameter have no effect with regard to that. If 0, 
no extra threads are stated and the search is done in one thread.
"""
        )
        .asInt(dft.searchThreadPoolSize());
    // Dynamic Search Window
    this.stopTransferCost =
      c
        .of("stopTransferCost")
        .since(NA)
        .summary("Use this to set a stop transfer cost for the given transfer priority")
        .description(
          """
The cost is applied to boarding and alighting at all stops. All stops have a transfer cost priority
set, the default is `ALLOWED`. The `stopTransferCost` parameter is optional, but if listed all 
values must be set.
          
If not set the `stopTransferCost` is ignored. This is only available for NeTEx imported Stops.
          
The cost is a scalar, but is equivalent to the felt cost of riding a transit trip for 1 second.
          
| Config key    | Description                                                                                   | Type |
|---------------|-----------------------------------------------------------------------------------------------|:----:|
| `DISCOURAGED` | Use a very high cost like `72 000` to eliminate transfers at the stop if not the only option. | int  |
| `ALLOWED`     | Allowed, but not recommended. Use something like `150`.                                       | int  |
| `RECOMMENDED` | Use a small cost penalty like `60`.                                                           | int  |
| `PREFERRED`   | The best place to do transfers. Should be set to `0`(zero).                                   | int  |
          
Use values in a range from `0` to `100 000`. **All key/value pairs are required if the 
`stopTransferCost` is listed.**
"""
        )
        .asEnumMapAllKeysRequired(StopTransferPriority.class, Integer.class);
    this.transferCacheMaxSize =
      c
        .of("transferCacheMaxSize")
        .since(NA)
        .summary(
          "The maximum number of distinct transfers parameters to cache pre-calculated transfers for."
        )
        .description(
          " If too low, requests may be slower. If too high, more memory may be used then required."
        )
        .asInt(25);

    this.pagingSearchWindowAdjustments =
      c
        .of("pagingSearchWindowAdjustments")
        .since(NA)
        .summary(
          "The provided array of durations is used to increase the search-window for the " +
          "next/previous page."
        )
        .description(
          """
The search window is expanded when the current page return few options. If ZERO results is returned
the first duration in the list is used, if ONE result is returned then the second duration is used
and so on. The duration is added to the existing search-window and inserted into the next and 
previous page cursor. See JavaDoc for [TransitTuningParameters#pagingSearchWindowAdjustments](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/routing/algorithm/raptor/transit/TransitTuningParameters.java)" +
for more info."
"""
        )
        .asDurations(PAGING_SEARCH_WINDOW_ADJUSTMENTS);

    this.dynamicSearchWindowCoefficients = new DynamicSearchWindowConfig("dynamicSearchWindow", c);
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

    public DynamicSearchWindowConfig(String parameterName, NodeAdapter root) {
      var dsWin = root
        .of("dynamicSearchWindow")
        .since(NA)
        .summary("The dynamic search window coefficients used to calculate the EDT, LAT and SW.")
        .description(
          "EDT is earliest-departure-time, LAT is latest-arrival-time and SW is " +
          "raptor-search-window using heuristics."
        )
        .asObject();

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
