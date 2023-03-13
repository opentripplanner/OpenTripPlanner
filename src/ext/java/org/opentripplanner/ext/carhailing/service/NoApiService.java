package org.opentripplanner.ext.carhailing.service;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.carhailing.CarHailingService;
import org.opentripplanner.ext.carhailing.model.ArrivalTime;
import org.opentripplanner.ext.carhailing.model.CarHailingCompany;
import org.opentripplanner.ext.carhailing.model.RideEstimate;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.basic.Money;

/**
 * This data source is to model a transportation network company for which no API exists to calculate real-time arrival
 * estimates or ride estimates. The config for this updater can include a default value for the estimated arrival time
 * which will always be provided when estimates for the arrival time are desired.
 */
public class NoApiService extends CarHailingService {

  /**
   * The default arrival time in seconds to respond with for all valid arrival time estimate requests. Defaults to 0.
   */
  private final Duration DEFAULT_ARRIVAL_DURATION = Duration.ZERO;

  /**
   * Whether or not the TNC service being modeled is wheelchair accessible. Defaults to false.
   */
  private boolean isWheelChairAccessible = false;

  /**
   * For testing purposes only.
   */
  public NoApiService(boolean isWheelChairAccessible) {
    this.isWheelChairAccessible = isWheelChairAccessible;
  }

  @Override
  public CarHailingCompany carHailingCompany() {
    return CarHailingCompany.NOAPI;
  }

  /**
   * In lieu of making an API request, this will merely return a single arrival estimate with the default arrive time.
   * TODO: add the ability to parse GeoJSON representing the service area of the TNC provider in order to be able to
   *  determine if TNC service is unavailable at the given position.
   */
  @Override
  protected List<ArrivalTime> queryArrivalTimes(WgsCoordinate position) {
    return List.of(
      new ArrivalTime(
        CarHailingCompany.NOAPI,
        "no-api-tnc-service",
        "no-api-tnc-service",
        DEFAULT_ARRIVAL_DURATION,
        isWheelChairAccessible
      )
    );
  }

  /**
   * In lieu of making an API request, this will merely return a single ride estimate with 0 duration and cost, but
   * with the wheelchair accessibility type that is possibly defined in the config.
   */
  @Override
  protected List<RideEstimate> queryRideEstimates(RideEstimateRequest request) {
    return List.of(
      new RideEstimate(
        CarHailingCompany.NOAPI,
        Duration.ZERO,
        Money.usDollars(0),
        Money.usDollars(0),
        "no-api-tnc-service",
        isWheelChairAccessible
      )
    );
  }
}
