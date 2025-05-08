package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class JourneyRequest implements Cloneable, Serializable {

  private static final JourneyRequest DEFAULT = new JourneyRequest();

  private TransitRequest transit = TransitRequest.DEFAULT;
  private StreetRequest access = StreetRequest.DEFAULT;
  private StreetRequest egress = StreetRequest.DEFAULT;
  private StreetRequest transfer = StreetRequest.DEFAULT;
  private StreetRequest direct = StreetRequest.DEFAULT;

  public TransitRequest transit() {
    return transit;
  }

  public JourneyRequest withTransit(Consumer<TransitRequestBuilder> body) {
    this.transit = transit.copyOf().apply(body).build();
    return this;
  }

  public StreetRequest access() {
    return access;
  }

  public JourneyRequest withAccess(StreetRequest access) {
    this.access = access;
    return this;
  }

  public StreetRequest egress() {
    return egress;
  }

  public JourneyRequest withEgress(StreetRequest egress) {
    this.egress = egress;
    return this;
  }

  public StreetRequest transfer() {
    return transfer;
  }

  public JourneyRequest withTransfer(StreetRequest transfer) {
    this.transfer = transfer;
    return this;
  }

  public StreetRequest direct() {
    return direct;
  }

  public JourneyRequest withDirect(StreetRequest direct) {
    this.direct = direct;
    return this;
  }

  /** Set access, egress, transfer and direcet mode to a given mode. */
  public void setAllModes(StreetMode mode) {
    var value = new StreetRequest(mode);
    withAccess(value);
    withEgress(value);
    withTransfer(value);
    withDirect(value);
  }

  public void setModes(RequestModes modes) {
    withAccess(new StreetRequest(modes.accessMode));
    withEgress(new StreetRequest(modes.egressMode));
    withTransfer(new StreetRequest(modes.transferMode));
    withDirect(new StreetRequest(modes.directMode));
  }

  public RequestModes modes() {
    return RequestModes.of()
      .withAccessMode(access.mode())
      .withEgressMode(egress.mode())
      .withTransferMode(transfer.mode())
      .withDirectMode(direct.mode())
      .build();
  }

  public JourneyRequest clone() {
    try {
      var clone = (JourneyRequest) super.clone();

      // No need to clone immutable objects
      clone.transit = this.transit;
      clone.access = this.access;
      clone.egress = this.egress;
      clone.transfer = this.transfer;
      clone.direct = this.direct;

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JourneyRequest that = (JourneyRequest) o;
    return (
      Objects.equals(transit, that.transit) &&
      Objects.equals(access, that.access) &&
      Objects.equals(egress, that.egress) &&
      Objects.equals(transfer, that.transfer) &&
      Objects.equals(direct, that.direct)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(transit, access, egress, transfer, direct);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(JourneyRequest.class)
      .addObj("transit", transit, DEFAULT.transit)
      .addObj("access", access, DEFAULT.access)
      .addObj("egress", egress, DEFAULT.egress)
      .addObj("transfer", transfer, DEFAULT.transfer)
      .addObj("direct", direct, DEFAULT.direct)
      .toString();
  }
}
