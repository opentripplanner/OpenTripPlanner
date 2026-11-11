package org.opentripplanner.ext.carpooling;

public class CarpoolingParametersTestData {

  /** The defaults with another limit on a feed's live trips. */
  public static CarpoolingParameters withMaxTrips(int maxTrips) {
    var d = CarpoolingParameters.DEFAULT;
    return new CarpoolingParameters(
      d.maxCandidateTripsPerRequest(),
      maxTrips,
      d.maxPagesPerPoll(),
      d.maxStopWalk(),
      d.maxTripDuration(),
      d.tripExpiry(),
      d.expirySweepInterval(),
      d.maxRoutePointSnap(),
      d.minCarEscapeMeters(),
      d.defaultSearchWindow()
    );
  }

  /** The defaults with another limit on the pages one poll reads. */
  public static CarpoolingParameters withMaxPagesPerPoll(int maxPagesPerPoll) {
    var d = CarpoolingParameters.DEFAULT;
    return new CarpoolingParameters(
      d.maxCandidateTripsPerRequest(),
      d.maxTrips(),
      maxPagesPerPoll,
      d.maxStopWalk(),
      d.maxTripDuration(),
      d.tripExpiry(),
      d.expirySweepInterval(),
      d.maxRoutePointSnap(),
      d.minCarEscapeMeters(),
      d.defaultSearchWindow()
    );
  }
}
