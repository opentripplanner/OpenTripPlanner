package org.opentripplanner.osm.model;

public class OsmTag {

  private String k;
  private String v;

  public OsmTag() {}

  public OsmTag(String k, String v) {
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
