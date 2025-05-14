package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class JourneyRequest implements Serializable {

  public static final JourneyRequest DEFAULT = new JourneyRequest(
    TransitRequest.DEFAULT,
    StreetRequest.DEFAULT,
    StreetRequest.DEFAULT,
    StreetRequest.DEFAULT,
    StreetRequest.DEFAULT,
    false
  );

  private final TransitRequest transit;
  private final StreetRequest access;
  private final StreetRequest egress;
  private final StreetRequest transfer;
  private final StreetRequest direct;
  private final boolean wheelchair;

  JourneyRequest(
    TransitRequest transit,
    StreetRequest access,
    StreetRequest egress,
    StreetRequest transfer,
    StreetRequest direct,
    boolean wheelchair
  ) {
    this.transit = transit;
    this.access = access;
    this.egress = egress;
    this.transfer = transfer;
    this.direct = direct;
    this.wheelchair = wheelchair;
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

  /**
   * Whether the trip must be wheelchair-accessible
   */
  public boolean wheelchair() {
    return wheelchair;
  }

  public RequestModes modes() {
    return RequestModes.of()
      .withAccessMode(access.mode())
      .withEgressMode(egress.mode())
      .withTransferMode(transfer.mode())
      .withDirectMode(direct.mode())
      .build();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JourneyRequest that = (JourneyRequest) o;
    return (
      wheelchair == that.wheelchair &&
      Objects.equals(transit, that.transit) &&
      Objects.equals(access, that.access) &&
      Objects.equals(egress, that.egress) &&
      Objects.equals(transfer, that.transfer) &&
      Objects.equals(direct, that.direct)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(transit, access, egress, transfer, direct, wheelchair);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(JourneyRequest.class)
      .addObj("transit", transit, DEFAULT.transit)
      .addObj("access", access, DEFAULT.access)
      .addObj("egress", egress, DEFAULT.egress)
      .addObj("transfer", transfer, DEFAULT.transfer)
      .addObj("direct", direct, DEFAULT.direct)
      .addBoolIfTrue("wheelchair", wheelchair)
      .toString();
  }
}
