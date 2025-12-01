package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class CarpoolTripBuilder extends AbstractEntityBuilder<CarpoolTrip, CarpoolTripBuilder> {

  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private String provider;

  private Duration deviationBudget = Duration.ofMinutes(15);
  private int availableSeats = 1;
  private List<CarpoolStop> stops = new ArrayList<>();

  public CarpoolTripBuilder(CarpoolTrip original) {
    super(original);
    this.startTime = original.startTime();
    this.endTime = original.endTime();
    this.provider = original.provider();
    this.deviationBudget = original.deviationBudget();
    this.availableSeats = original.availableSeats();
    this.stops = new ArrayList<>(original.stops());
  }

  public CarpoolTripBuilder(FeedScopedId id) {
    super(id);
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

  public CarpoolTripBuilder withDeviationBudget(Duration deviationBudget) {
    this.deviationBudget = deviationBudget;
    return this;
  }

  public CarpoolTripBuilder withAvailableSeats(int availableSeats) {
    this.availableSeats = availableSeats;
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

  public Duration deviationBudget() {
    return deviationBudget;
  }

  public int availableSeats() {
    return availableSeats;
  }

  public CarpoolTripBuilder withStops(List<CarpoolStop> stops) {
    this.stops = new ArrayList<>(stops);
    return this;
  }

  public CarpoolTripBuilder addStop(CarpoolStop stop) {
    this.stops.add(stop);
    // Sort stops by sequence number to maintain order
    this.stops.sort((a, b) -> Integer.compare(a.getSequenceNumber(), b.getSequenceNumber()));
    return this;
  }

  public CarpoolTripBuilder clearStops() {
    this.stops.clear();
    return this;
  }

  public List<CarpoolStop> stops() {
    return stops;
  }

  @Override
  protected CarpoolTrip buildFromValues() {
    validateStopSequence();

    return new CarpoolTrip(this);
  }

  private void validateStopSequence() {
    for (int i = 0; i < stops.size(); i++) {
      CarpoolStop stop = stops.get(i);
      if (stop.getSequenceNumber() != i) {
        throw new IllegalStateException(
          String.format(
            "Stop sequence mismatch: expected %d but got %d at position %d",
            i,
            stop.getSequenceNumber(),
            i
          )
        );
      }
    }
  }
}
