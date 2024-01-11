package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import org.opentripplanner.routing.api.request.StreetMode;

// TODO VIA: Javadoc
public class StreetRequest implements Cloneable, Serializable {

  private StreetMode mode;

  public StreetRequest() {
    this(StreetMode.WALK);
  }

  public StreetRequest(StreetMode mode) {
    this.mode = mode;
  }

  public void setMode(StreetMode mode) {
    this.mode = mode;
  }

  public StreetMode mode() {
    return mode;
  }

  public StreetRequest clone() {
    try {
      return (StreetRequest) super.clone();
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
