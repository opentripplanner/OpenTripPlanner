package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;

public class CarpoolTripBuilder extends AbstractEntityBuilder<CarpoolTrip, CarpoolTripBuilder> {

  private AreaStop boardingArea;
  private AreaStop alightingArea;
  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private String provider;

  private Duration deviationBudget = Duration.ofMinutes(15);
  private int availableSeats = 1;

  public CarpoolTripBuilder(CarpoolTrip original) {
    super(original);
    this.boardingArea = original.boardingArea();
    this.alightingArea = original.alightingArea();
    this.startTime = original.startTime();
    this.endTime = original.endTime();
    this.provider = original.provider();
    this.deviationBudget = original.deviationBudget();
    this.availableSeats = original.availableSeats();
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

  public AreaStop boardingArea() {
    return boardingArea;
  }

  public AreaStop alightingArea() {
    return alightingArea;
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

  @Override
  protected CarpoolTrip buildFromValues() {
    return new CarpoolTrip(this);
  }
}
