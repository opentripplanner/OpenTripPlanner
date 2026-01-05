package org.onebusaway.gtfs.model;

public class AgencyAndIdFactory {

  private static final String AGENCY_ID = "oba";

  public static AgencyAndId obaId(String id) {
    return new AgencyAndId(AGENCY_ID, id);
  }
}
