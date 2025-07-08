package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.function.Consumer;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

public class JourneyRequestBuilder implements Serializable {

  private TransitRequest transit;
  private StreetRequest access;
  private StreetRequest egress;
  private StreetRequest transfer;
  private StreetRequest direct;
  private boolean wheelchair;

  private final JourneyRequest original;

  JourneyRequestBuilder(JourneyRequest original) {
    this.original = original;
    this.transit = original.transit();
    this.access = original.access();
    this.egress = original.egress();
    this.transfer = original.transfer();
    this.direct = original.direct();
    this.wheelchair = original.wheelchair();
  }

  public JourneyRequestBuilder withTransit(TransitRequest transit) {
    this.transit = transit;
    return this;
  }

  public JourneyRequestBuilder withTransit(Consumer<TransitRequestBuilder> body) {
    return withTransit(transit.copyOf().apply(body).build());
  }

  public JourneyRequestBuilder withAccess(StreetRequest access) {
    this.access = access;
    return this;
  }

  public JourneyRequestBuilder withEgress(StreetRequest egress) {
    this.egress = egress;
    return this;
  }

  public JourneyRequestBuilder withTransfer(StreetRequest transfer) {
    this.transfer = transfer;
    return this;
  }

  public JourneyRequestBuilder withDirect(StreetRequest direct) {
    this.direct = direct;
    return this;
  }

  public JourneyRequestBuilder withoutDirect() {
    return withDirect(new StreetRequest(StreetMode.NOT_SET));
  }

  public JourneyRequestBuilder withWheelchair(boolean wheelchair) {
    this.wheelchair = wheelchair;
    return this;
  }

  /**
   * Set access, egress, transfer and direcet mode to a given mode.
   */
  public JourneyRequestBuilder setAllModes(StreetMode mode) {
    var value = new StreetRequest(mode);
    withAccess(value);
    withEgress(value);
    withTransfer(value);
    withDirect(value);
    return this;
  }

  public JourneyRequestBuilder setModes(RequestModes modes) {
    withAccess(new StreetRequest(modes.accessMode));
    withEgress(new StreetRequest(modes.egressMode));
    withTransfer(new StreetRequest(modes.transferMode));
    withDirect(new StreetRequest(modes.directMode));
    return this;
  }

  public JourneyRequestBuilder apply(Consumer<JourneyRequestBuilder> body) {
    body.accept(this);
    return this;
  }

  public JourneyRequest build() {
    var value = new JourneyRequest(transit, access, egress, transfer, direct, wheelchair);
    return original.equals(value) ? original : value;
  }
}
