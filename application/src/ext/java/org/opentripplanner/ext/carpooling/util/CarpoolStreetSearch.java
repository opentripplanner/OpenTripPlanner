package org.opentripplanner.ext.carpooling.util;

import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/** The street search request every carpool car search uses, so they all rank paths alike. */
public final class CarpoolStreetSearch {

  private CarpoolStreetSearch() {}

  public static StreetSearchRequest carRequest(boolean arriveBy) {
    return StreetSearchRequest.of().withMode(StreetMode.CAR).withArriveBy(arriveBy).build();
  }
}
