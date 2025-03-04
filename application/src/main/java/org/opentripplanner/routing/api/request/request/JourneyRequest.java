package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import org.opentripplanner.routing.api.request.RequestModes;

// TODO VIA: Javadoc
public class JourneyRequest implements Cloneable, Serializable {

  private TransitRequest transit = new TransitRequest();
  private StreetRequest access = new StreetRequest();
  private StreetRequest egress = new StreetRequest();
  private StreetRequest transfer = new StreetRequest();
  private StreetRequest direct = new StreetRequest();

  public TransitRequest transit() {
    return transit;
  }

  public StreetRequest access() {
    return access;
  }

  public StreetRequest egress() {
    return egress;
  }

  public StreetRequest transfer() {
    return transfer;
  }

  public StreetRequest direct() {
    return direct;
  }

  public void setModes(RequestModes modes) {
    transfer().setMode(modes.transferMode);
    access().setMode(modes.accessMode);
    egress().setMode(modes.egressMode);
    direct().setMode(modes.directMode);
  }

  public RequestModes modes() {
    return RequestModes.of()
      .withAccessMode(access.mode())
      .withTransferMode(transfer.mode())
      .withEgressMode(egress.mode())
      .withDirectMode(direct.mode())
      .build();
  }

  public JourneyRequest clone() {
    try {
      var clone = (JourneyRequest) super.clone();
      clone.transit = this.transit.clone();
      clone.access = this.access.clone();
      clone.egress = this.egress.clone();
      clone.transfer = this.transfer.clone();
      clone.direct = this.direct.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
