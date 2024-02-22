package org.opentripplanner.ext.restapi.model;

import java.io.Serializable;

public class ApiTripTimeShort implements Serializable {

  private static final int UNDEFINED = -1;

  public String stopId;
  public int stopIndex;
  public int stopCount;
  public int scheduledArrival = UNDEFINED;
  public int scheduledDeparture = UNDEFINED;
  public int realtimeArrival = UNDEFINED;
  public int realtimeDeparture = UNDEFINED;
  public int arrivalDelay = UNDEFINED;
  public int departureDelay = UNDEFINED;
  public boolean timepoint = false;
  public boolean realtime = false;
  public ApiRealTimeState realtimeState = ApiRealTimeState.SCHEDULED;
  public long serviceDay;
  public String tripId;
  public String blockId;
  public String headsign;
}
