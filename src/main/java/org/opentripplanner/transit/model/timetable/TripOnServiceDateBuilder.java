package org.opentripplanner.transit.model.timetable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripOnServiceDateBuilder
  extends AbstractEntityBuilder<TripOnServiceDate, TripOnServiceDateBuilder> {

  private Trip trip;
  private LocalDate serviceDate;
  private TripAlteration tripAlteration;
  private List<TripOnServiceDate> replacementFor = List.of();

  TripOnServiceDateBuilder(FeedScopedId id) {
    super(id);
  }

  TripOnServiceDateBuilder(TripOnServiceDate original) {
    super(original);
    this.trip = original.getTrip();
    this.serviceDate = original.getServiceDate();
    this.tripAlteration = original.getTripAlteration();
    this.replacementFor = new ArrayList<>(original.getReplacementFor());
  }

  public TripOnServiceDateBuilder withTrip(Trip trip) {
    this.trip = trip;
    return this;
  }

  public TripOnServiceDateBuilder withServiceDate(LocalDate serviceDate) {
    this.serviceDate = serviceDate;
    return this;
  }

  public TripOnServiceDateBuilder withTripAlteration(TripAlteration tripAlteration) {
    this.tripAlteration = tripAlteration;
    return this;
  }

  public TripOnServiceDateBuilder withReplacementFor(List<TripOnServiceDate> replacementFor) {
    this.replacementFor = replacementFor;
    return this;
  }

  public Trip getTrip() {
    return trip;
  }

  public LocalDate getServiceDate() {
    return serviceDate;
  }

  public TripAlteration getTripAlteration() {
    return tripAlteration;
  }

  public List<TripOnServiceDate> getReplacementFor() {
    return replacementFor;
  }

  @Override
  protected TripOnServiceDate buildFromValues() {
    return new TripOnServiceDate(this);
  }
}
