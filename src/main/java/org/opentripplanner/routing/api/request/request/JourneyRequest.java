package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;

// TODO VIA: Javadoc
public class JourneyRequest implements Cloneable, Serializable {

  private TransitRequest transit = new TransitRequest();

  public TransitRequest transit() {
    return transit;
  }

  public JourneyRequest clone() {
    try {
      var clone = (JourneyRequest) super.clone();
      clone.transit = this.transit.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
