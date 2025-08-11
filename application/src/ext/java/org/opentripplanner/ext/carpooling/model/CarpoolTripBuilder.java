package org.opentripplanner.ext.carpooling.model;

import java.time.ZonedDateTime;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.timetable.Trip;

public class CarpoolTripBuilder extends AbstractEntityBuilder<CarpoolTrip, CarpoolTripBuilder> {

  private AreaStop boardingArea;
  private AreaStop alightingArea;
  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private Trip trip;
  private String provider;
  private int availableSeats = 1;

  public CarpoolTripBuilder(CarpoolTrip original) {
    super(original);
    this.boardingArea = original.getBoardingArea();
    this.alightingArea = original.getAlightingArea();
    this.startTime = original.getStartTime();
    this.endTime = original.getEndTime();
    this.trip = original.getTrip();
    this.provider = original.getProvider();
    this.availableSeats = original.getAvailableSeats();
  }

  public CarpoolTripBuilder(FeedScopedId id) {
    super(id);
  }

  public CarpoolTripBuilder withBoardingArea(AreaStop boardingArea) {
    this.boardingArea = boardingArea;
    return this;
  }

  public CarpoolTripBuilder withAlightingArea(AreaStop alightingArea) {
    this.alightingArea = alightingArea;
    return this;
  }

  public CarpoolTripBuilder withStartTime(ZonedDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  public CarpoolTripBuilder withEndTime(ZonedDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  public CarpoolTripBuilder withTrip(Trip trip) {
    this.trip = trip;
    return this;
  }

  public CarpoolTripBuilder withProvider(String provider) {
    this.provider = provider;
    return this;
  }

  public CarpoolTripBuilder withAvailableSeats(int availableSeats) {
    this.availableSeats = availableSeats;
    return this;
  }

  public AreaStop getBoardingArea() {
    return boardingArea;
  }

  public AreaStop getAlightingArea() {
    return alightingArea;
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public ZonedDateTime getEndTime() {
    return endTime;
  }

  public Trip getTrip() {
    return trip;
  }

  public String getProvider() {
    return provider;
  }

  public int getAvailableSeats() {
    return availableSeats;
  }

  @Override
  protected CarpoolTrip buildFromValues() {
    return new CarpoolTrip(this);
  }
}
