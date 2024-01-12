package org.opentripplanner.ext.restapi.model;

import java.io.Serializable;
import java.util.Objects;

public final class ApiFeedInfo implements Serializable {

  public String id;
  public String publisherName;
  public String publisherUrl;
  public String lang;
  public String startDate;
  public String endDate;
  public String version;

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
    ApiFeedInfo that = (ApiFeedInfo) o;
    return id.equals(that.id);
  }

  public String toString() {
    return "<FeedInfo " + id + ">";
  }
}
