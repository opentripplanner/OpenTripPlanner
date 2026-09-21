package org.opentripplanner.ext.carpooling.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.organization.ContactInfo;

/**
 * Builder for {@link CarpoolTrip} instances.
 */
public class CarpoolTripBuilder extends AbstractEntityBuilder<CarpoolTrip, CarpoolTripBuilder> {

  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private String provider;
  private int totalCapacity = CarpoolTrip.DEFAULT_TOTAL_CAPACITY;
  private List<CarpoolStop> stops = new ArrayList<>();

  @Nullable
  private ContactInfo publicContactInformation;

  public CarpoolTripBuilder(FeedScopedId id) {
    super(id);
  }

  public CarpoolTripBuilder(CarpoolTrip original) {
    super(original);
    this.startTime = original.startTime();
    this.endTime = original.endTime();
    this.provider = original.provider();
    this.totalCapacity = original.totalCapacity();
    this.stops = new ArrayList<>(original.stops());
    this.publicContactInformation = original.publicContactInformation();
  }

  public CarpoolTripBuilder withStartTime(ZonedDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  public CarpoolTripBuilder withEndTime(ZonedDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  public CarpoolTripBuilder withProvider(String provider) {
    this.provider = provider;
    return this;
  }

  public CarpoolTripBuilder withTotalCapacity(int totalCapacity) {
    this.totalCapacity = totalCapacity;
    return this;
  }

  public ZonedDateTime startTime() {
    return startTime;
  }

  public ZonedDateTime endTime() {
    return endTime;
  }

  public String provider() {
    return provider;
  }

  public int totalCapacity() {
    return totalCapacity;
  }

  public CarpoolTripBuilder withPublicContactInformation(
    @Nullable ContactInfo publicContactInformation
  ) {
    this.publicContactInformation = publicContactInformation;
    return this;
  }

  @Nullable
  public ContactInfo publicContactInformation() {
    return publicContactInformation;
  }

  public CarpoolTripBuilder withStops(List<CarpoolStop> stops) {
    this.stops = new ArrayList<>(stops);
    return this;
  }

  public List<CarpoolStop> stops() {
    return stops;
  }

  @Override
  protected CarpoolTrip buildFromValues() {
    return new CarpoolTrip(this);
  }
}
