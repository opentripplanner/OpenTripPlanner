package org.opentripplanner.routing.api.request.preference;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.PatternCostCalculator.DEFAULT_ROUTE_RELUCTANCE;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import org.opentripplanner.routing.api.request.RaptorOptions;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.routing.api.request.framework.DurationForEnumBuilder;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Preferences for transit routing.
 */
public class TransitPreferences implements Cloneable, Serializable {

  private DurationForEnum<TransitMode> boardSlack = DurationForEnum.of(TransitMode.class).build();
  private DurationForEnum<TransitMode> alightSlack = DurationForEnum.of(TransitMode.class).build();

  private Map<TransitMode, Double> reluctanceForMode = new HashMap<>();

  private int otherThanPreferredRoutesPenalty = 300;

  private DoubleFunction<Double> unpreferredCost = RequestFunctions.createLinearFunction(
    0.0,
    DEFAULT_ROUTE_RELUCTANCE
  );

  private boolean ignoreRealtimeUpdates = false;
  private boolean includePlannedCancellations = false;

  private RaptorOptions raptorOptions = new RaptorOptions();

  /**
   * Has information how much time boarding a vehicle takes; The number of seconds to add before
   * boarding a transit leg. Can be significant for airplanes or ferries. It is recommended to use
   * the `boardTimes` in the `router-config.json` to set this for each mode.
   * <p>
   * Board-slack can be configured per mode, if not set for a given mode it falls back to the
   * default value. This enables configuring the board-slack for airplane boarding to be 30 minutes
   * and a 2 minutes slack for everything else.
   * <p>
   * Unit is seconds. Default value is 0.
   */
  public DurationForEnum<TransitMode> boardSlack() {
    return boardSlack;
  }

  public void initBoardSlack(Duration defaultValue, Map<TransitMode, Duration> values) {
    withBoardSlack(builder -> builder.withDefault(defaultValue).withValues(values));
  }

  public TransitPreferences withBoardSlack(Consumer<DurationForEnumBuilder<TransitMode>> body) {
    this.boardSlack = this.boardSlack.copyOf(body);
    return this;
  }

  /**
   * Has information how much time alighting a vehicle takes; The number of seconds to add after
   * alighting a transit leg. Can be significant for airplanes or ferries.  It is recommended to
   * use the `alightTimes` in the `router-config.json` to set this for each mode.
   * <p>
   * Alight-slack can be configured per mode. The default value is used if not set for a given mode.
   * This enables configuring the alight-slack for train alighting to be 4 minutes and a bus alight
   * slack to be 0 minutes.
   * <p>
   * Unit is seconds. Default value is 0.
   */
  public DurationForEnum<TransitMode> alightSlack() {
    return alightSlack;
  }

  public void initAlightSlack(Duration defaultValue, Map<TransitMode, Duration> values) {
    withAlightSlack(builder -> builder.withDefault(defaultValue).withValues(values));
  }

  public TransitPreferences withAlightSlack(Consumer<DurationForEnumBuilder<TransitMode>> body) {
    this.alightSlack = this.alightSlack.copyOf(body);
    return this;
  }

  /**
   * Transit reluctance per mode. Use this to add a advantage(<1.0) to specific modes, or to add a
   * penalty to other modes (> 1.0). The type used here it the internal model {@link TransitMode}
   * make sure to create a mapping for this before using it on the API.
   * <p>
   * If set, the alight-slack-for-mode override the default value {@code 1.0}.
   * <p>
   * This is a scalar multiplied with the time in second on board the transit vehicle. Default value
   * is not-set(empty map).
   */
  public Map<TransitMode, Double> reluctanceForMode() {
    return reluctanceForMode;
  }

  public void setReluctanceForMode(Map<TransitMode, Double> reluctanceForMode) {
    this.reluctanceForMode = reluctanceForMode;
  }

  @Deprecated
  public void setOtherThanPreferredRoutesPenalty(int otherThanPreferredRoutesPenalty) {
    this.otherThanPreferredRoutesPenalty = otherThanPreferredRoutesPenalty;
  }

  /**
   * Penalty added for using every route that is not preferred if user set any route as preferred.
   * We return number of seconds that we are willing to wait for preferred route.
   *
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  public int otherThanPreferredRoutesPenalty() {
    return otherThanPreferredRoutesPenalty;
  }

  /**
   * A cost function used to calculate penalty for an unpreferred route. Function should return
   * number of seconds that we are willing to wait for preferred route.
   */
  public DoubleFunction<Double> unpreferredCost() {
    return unpreferredCost;
  }

  public void setUnpreferredCost(DoubleFunction<Double> unpreferredCost) {
    this.unpreferredCost = unpreferredCost;
  }

  public void setUnpreferredCostString(String constFunction) {
    unpreferredCost = RequestFunctions.parse(constFunction);
  }

  /**
   * When true, realtime updates are ignored during this search.
   */
  public boolean ignoreRealtimeUpdates() {
    return ignoreRealtimeUpdates;
  }

  public void setIgnoreRealtimeUpdates(boolean ignoreRealtimeUpdates) {
    this.ignoreRealtimeUpdates = ignoreRealtimeUpdates;
  }

  public void setIncludePlannedCancellations(boolean includePlannedCancellations) {
    this.includePlannedCancellations = includePlannedCancellations;
  }

  /**
   * When true, trips cancelled in scheduled data are included in this search.
   */
  public boolean includePlannedCancellations() {
    return includePlannedCancellations;
  }

  /**
   * Set of options to use with Raptor. These are available here for testing purposes.
   */
  public RaptorOptions raptorOptions() {
    return raptorOptions;
  }

  public TransitPreferences clone() {
    try {
      // TODO VIA (Thomas): 2022-08-26 skipping unpreferredRouteCost (that's how it was before)
      var clone = (TransitPreferences) super.clone();

      clone.boardSlack = this.boardSlack;
      clone.alightSlack = alightSlack;
      clone.reluctanceForMode = new HashMap<>(reluctanceForMode);
      clone.raptorOptions = new RaptorOptions(raptorOptions);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
