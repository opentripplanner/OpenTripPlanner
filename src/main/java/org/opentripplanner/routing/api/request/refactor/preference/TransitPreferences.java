package org.opentripplanner.routing.api.request.refactor.preference;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RouteCostCalculator.DEFAULT_ROUTE_RELUCTANCE;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleFunction;
import org.opentripplanner.routing.api.request.RaptorOptions;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.transit.model.basic.TransitMode;

public class TransitPreferences implements Cloneable, Serializable {

  /**
   * When true, realtime updates are ignored during this search.
   */
  private boolean ignoreRealtimeUpdates = false;
  /**
   * When true, trips cancelled in scheduled data are included in this search.
   */
  private boolean includePlannedCancellations = false;
  /**
   * The number of seconds to add before boarding a transit leg. It is recommended to use the
   * `boardTimes` in the `router-config.json` to set this for each mode.
   * <p>
   * Unit is seconds. Default value is 0.
   */
  private int boardSlack;
  /**
   * Has information how much time boarding a vehicle takes. Can be significant eg in airplanes or
   * ferries.
   * <p>
   * If set, the board-slack-for-mode override the more general {@link #boardSlack}. This enables
   * configuring the board-slack for airplane boarding to be 30 minutes and a slack for bus of 2
   * minutes.
   * <p>
   * Unit is seconds. Default value is not-set(empty map).
   */
  private Map<TransitMode, Integer> boardSlackForMode = new EnumMap<TransitMode, Integer>(
    TransitMode.class
  );
  /**
   * The number of seconds to add after alighting a transit leg. It is recommended to use the
   * `alightTimes` in the `router-config.json` to set this for each mode.
   * <p>
   * Unit is seconds. Default value is 0.
   */
  private int alightSlack = 0;
  /**
   * Has information how much time alighting a vehicle takes. Can be significant eg in airplanes or
   * ferries.
   * <p>
   * If set, the alight-slack-for-mode override the more general {@link #alightSlack}. This enables
   * configuring the alight-slack for train alighting to be 4 minutes and a bus alight slack to be 0
   * minutes.
   * <p>
   * Unit is seconds. Default value is not-set(empty map).
   */
  private Map<TransitMode, Integer> alightSlackForMode = new EnumMap<TransitMode, Integer>(
    TransitMode.class
  );
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
  private Map<TransitMode, Double> reluctanceForMode = new HashMap<>();

  /**
   * Penalty added for using every route that is not preferred if user set any route as preferred.
   * We return number of seconds that we are willing to wait for preferred route.
   *
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  private int otherThanPreferredRoutesPenalty = 300;

  /**
   * A cost function used to calculate penalty for an unpreferred route. Function should return
   * number of seconds that we are willing to wait for preferred route.
   */
  private DoubleFunction<Double> unpreferredRouteCost = RequestFunctions.createLinearFunction(
    0.0,
    DEFAULT_ROUTE_RELUCTANCE
  );

  /**
   * Set of options to use with Raptor. These are available here for testing purposes.
   */
  private RaptorOptions raptorOptions = new RaptorOptions();

  public void setIgnoreRealtimeUpdates(boolean ignoreRealtimeUpdates) {
    this.ignoreRealtimeUpdates = ignoreRealtimeUpdates;
  }

  public boolean ignoreRealtimeUpdates() {
    return ignoreRealtimeUpdates;
  }

  public void setIncludePlannedCancellations(boolean includePlannedCancellations) {
    this.includePlannedCancellations = includePlannedCancellations;
  }

  public boolean includePlannedCancellations() {
    return includePlannedCancellations;
  }

  public void setBoardSlack(int boardSlack) {
    this.boardSlack = boardSlack;
  }

  public int boardSlack() {
    return boardSlack;
  }

  public void setBoardSlackForMode(Map<TransitMode, Integer> boardSlackForMode) {
    this.boardSlackForMode = boardSlackForMode;
  }

  public Map<TransitMode, Integer> boardSlackForMode() {
    return boardSlackForMode;
  }

  public void setAlightSlack(int alightSlack) {
    this.alightSlack = alightSlack;
  }

  public int alightSlack() {
    return alightSlack;
  }

  public void setAlightSlackForMode(Map<TransitMode, Integer> alightSlackForMode) {
    this.alightSlackForMode = alightSlackForMode;
  }

  public Map<TransitMode, Integer> alightSlackForMode() {
    return alightSlackForMode;
  }

  public void setReluctanceForMode(Map<TransitMode, Double> reluctanceForMode) {
    this.reluctanceForMode = reluctanceForMode;
  }

  public Map<TransitMode, Double> reluctanceForMode() {
    return reluctanceForMode;
  }

  public void setOtherThanPreferredRoutesPenalty(int otherThanPreferredRoutesPenalty) {
    this.otherThanPreferredRoutesPenalty = otherThanPreferredRoutesPenalty;
  }

  public int otherThanPreferredRoutesPenalty() {
    return otherThanPreferredRoutesPenalty;
  }

  public RaptorOptions raptorOptions() {
    return raptorOptions;
  }

  public void setUnpreferredRouteCost(DoubleFunction<Double> unpreferredRouteCost) {
    this.unpreferredRouteCost = unpreferredRouteCost;
  }

  public void setUnpreferredRouteCostString(String constFunction) {
    unpreferredRouteCost = RequestFunctions.parse(constFunction);
  }

  public DoubleFunction<Double> unpreferredRouteCost() {
    return unpreferredRouteCost;
  }
}
