package org.opentripplanner.ext.carpooling.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_11;

import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class CarpoolingConfig {

  public static CarpoolingParameters mapParameters(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_11)
      .summary("Configuration for the street mode `carpool`.")
      .asObject();

    var d = CarpoolingParameters.DEFAULT;
    var boardCost = c
      .of("boardCost")
      .since(V2_11)
      .summary(
        "A cost added once to every carpool leg — direct, access and egress alike — for being picked up."
      )
      .asInt(d.boardCost());

    return new CarpoolingParameters(
      d.maxCandidateTripsPerRequest(),
      d.maxTrips(),
      d.maxPagesPerPoll(),
      d.maxStopWalk(),
      d.maxTripDuration(),
      d.tripExpiry(),
      d.expirySweepInterval(),
      d.maxRoutePointSnap(),
      d.minCarEscapeMeters(),
      d.defaultSearchWindow(),
      boardCost
    );
  }
}
