package org.opentripplanner.openstreetmap.model;

public class OSMTag {

  private String k;
  private String v;

  public OSMTag() {
  }

  public OSMTag(String k, String v) {
    this.k = k;
    this.v = v;
  }

  public String getK() {
    return k;
  }

  public void setK(String k) {
    this.k = k;
  }

  public String getV() {
    return v;
  }

  public void setV(String v) {
    this.v = v;
  }

  public String toString() {
    return k + "=" + v;
  }
}
