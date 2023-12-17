package org.opentripplanner.ext.restapi.model;

import java.io.Serializable;
import java.util.Objects;

public class ApiAgency implements Serializable {

  public String id;
  public String name;
  public String url;
  public String timezone;
  public String lang;
  public String phone;
  public String fareUrl;
  public String brandingUrl;

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiAgency apiAgency = (ApiAgency) o;
    return Objects.equals(id, apiAgency.id);
  }

  public String toString() {
    return "<Agency " + this.id + ">";
  }
}
