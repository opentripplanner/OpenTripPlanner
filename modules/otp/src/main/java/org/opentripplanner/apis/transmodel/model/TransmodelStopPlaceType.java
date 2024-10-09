package org.opentripplanner.apis.transmodel.model;

public enum TransmodelStopPlaceType {
  ONSTREET_BUS("onstreetBus"),
  ONSTREET_TRAM("onstreetTram"),
  AIRPORT("airport"),
  RAIL_STATION("railStation"),
  METRO_STATION("metroStation"),
  BUS_STATION("busStation"),
  COACH_STATION("coachStation"),
  TRAM_STATION("tramStation"),
  HARBOUR_PORT("harbourPort"),
  FERRY_PORT("ferryPort"),
  FERRY_STOP("ferryStop"),
  LIFT_STATION("liftStation"),
  VEHICLE_RAIL_INTERCHANGE("vehicleRailInterchange"),
  OTHER("other");

  private final String value;

  TransmodelStopPlaceType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
