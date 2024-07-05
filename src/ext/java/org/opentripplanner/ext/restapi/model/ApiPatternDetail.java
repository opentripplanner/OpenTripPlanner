package org.opentripplanner.ext.restapi.model;

import java.util.ArrayList;
import java.util.Collection;

public class ApiPatternDetail extends ApiPatternShort {

  /* Maybe these should just be lists of IDs only, since there are stops and trips subendpoints. */
  public Collection<ApiStopShort> stops = new ArrayList<>();
  public Collection<ApiTripShort> trips = new ArrayList<>();
}
