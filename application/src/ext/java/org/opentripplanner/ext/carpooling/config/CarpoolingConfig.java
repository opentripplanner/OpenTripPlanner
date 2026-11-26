package org.opentripplanner.ext.carpooling.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_11;

import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/** Maps the {@code carpooling} section of router-config.json. */
public class CarpoolingConfig {

  /** Returns {@link CarpoolingParameters#DEFAULT} with the configured values applied. */
  public static CarpoolingParameters mapParameters(String parameterName, NodeAdapter root) {
    var carpooling = root
      .of(parameterName)
      .since(V2_11)
      .summary("Configuration for the street mode `carpool`.")
      .asObject();

    var defaults = CarpoolingParameters.DEFAULT;
    var boardCost = carpooling
      .of("boardCost")
      .since(V2_11)
      .summary(
        "A cost added once to every carpool leg — direct, access and egress alike — for being picked up."
      )
      .asInt(defaults.boardCost());

    return new CarpoolingParameters(
      defaults.maxCandidateTripsPerRequest(),
      defaults.maxTrips(),
      defaults.maxPagesPerPoll(),
      defaults.maxStopWalk(),
      defaults.maxTripDuration(),
      defaults.tripExpiry(),
      defaults.expirySweepInterval(),
      defaults.maxRoutePointSnap(),
      defaults.minCarEscapeMeters(),
      defaults.defaultSearchWindow(),
      boardCost
    );
  }
}
