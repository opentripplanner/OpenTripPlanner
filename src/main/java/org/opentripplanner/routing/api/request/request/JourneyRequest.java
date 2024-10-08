package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import org.opentripplanner.routing.api.request.RequestModes;

// TODO VIA: Javadoc
public class JourneyRequest implements Cloneable, Serializable {

  private final TransitRequest transit;
  private final StreetRequest access;
  private final StreetRequest egress;
  private final StreetRequest transfer;
  private final StreetRequest direct;

  public JourneyRequest() {
    this.transit = new TransitRequest();
    this.access = new StreetRequest();
    this.egress = new StreetRequest();
    this.transfer = new StreetRequest();
    this.direct = new StreetRequest();
  }

  public JourneyRequest(
    TransitRequest transit,
    StreetRequest access,
    StreetRequest egress,
    StreetRequest transfer,
    StreetRequest direct
  ) {
    this.transit = transit;
    this.access = access;
    this.egress = egress;
    this.transfer = transfer;
    this.direct = direct;
  }

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
    return RequestModes
      .of()
      .withAccessMode(access.mode())
      .withTransferMode(transfer.mode())
      .withEgressMode(egress.mode())
      .withDirectMode(direct.mode())
      .build();
  }

  @Override
  public JourneyRequest clone() {
    return new JourneyRequest(
      this.transit.clone(),
      this.access.clone(),
      this.egress.clone(),
      this.transfer.clone(),
      this.direct.clone()
    );
  }
}
