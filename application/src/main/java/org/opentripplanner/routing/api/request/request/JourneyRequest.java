package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class JourneyRequest implements Cloneable, Serializable {

  public static final JourneyRequest DEFAULT = new JourneyRequest(
    TransitRequest.DEFAULT,
    StreetRequest.DEFAULT,
    StreetRequest.DEFAULT,
    StreetRequest.DEFAULT,
    StreetRequest.DEFAULT
  );

  private TransitRequest transit;
  private StreetRequest access;
  private StreetRequest egress;
  private StreetRequest transfer;
  private StreetRequest direct;

  JourneyRequest(
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

  public static JourneyRequestBuilder of() {
    return DEFAULT.copyOf();
  }

  public JourneyRequestBuilder copyOf() {
    return new JourneyRequestBuilder(this);
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
