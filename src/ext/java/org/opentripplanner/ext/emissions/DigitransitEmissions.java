package org.opentripplanner.ext.emissions;

public class DigitransitEmissions {

  private String db;
  private String agency_id;
  private String agency_name;
  private String mode;
  private String avg;
  private int p_avg;

  public DigitransitEmissions(
    String db,
    String agency_id,
    String agency_name,
    String mode,
    String avg,
    int p_avg
  ) {
    this.db = db;
    this.agency_id = agency_id;
    this.agency_name = agency_name;
    this.mode = mode;
    this.avg = avg;
    this.p_avg = p_avg;
  }

  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
  }

  public String getAgency_id() {
    return agency_id;
  }

  public void setAgency_id(String agency_id) {
    this.agency_id = agency_id;
  }

  public String getAgency_name() {
    return agency_name;
  }

  public void setAgency_name(String agency_name) {
    this.agency_name = agency_name;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getAvg() {
    return avg;
  }

  public void setAvg(String avg) {
    this.avg = avg;
  }

  public int getP_avg() {
    return p_avg;
  }

  public void setP_avg(int p_avg) {
    this.p_avg = p_avg;
  }
}
