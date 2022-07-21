/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or a
 * platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class StopBuilder extends StationElementBuilder<Stop, StopBuilder> {

  private String platformCode;

  private I18NString url;

  private ZoneId timeZone;

  private TransitMode gtfsVehicleType;

  private String netexVehicleSubmode;

  private final Set<BoardingArea> boardingAreas = new HashSet<>();

  private final Set<FareZone> fareZones = new HashSet<>();

  StopBuilder(FeedScopedId id) {
    super(id);
  }

  StopBuilder(Stop original) {
    super(original);
    this.platformCode = original.getPlatformCode();
    this.url = original.getUrl();
    this.timeZone = original.getTimeZone();
    this.gtfsVehicleType = original.getGtfsVehicleType();
    this.netexVehicleSubmode = original.getNetexVehicleSubmode().name();
  }

  public String platformCode() {
    return platformCode;
  }

  public StopBuilder withPlatformCode(String platformCode) {
    this.platformCode = platformCode;
    return this;
  }

  public I18NString url() {
    return url;
  }

  public StopBuilder withUrl(I18NString url) {
    this.url = url;
    return this;
  }

  public TransitMode vehicleType() {
    return gtfsVehicleType;
  }

  public StopBuilder withVehicleType(TransitMode vehicleType) {
    this.gtfsVehicleType = vehicleType;
    return this;
  }

  public String netexVehicleSubmode() {
    return netexVehicleSubmode;
  }

  public StopBuilder withNetexVehicleSubmode(String netexVehicleSubmode) {
    this.netexVehicleSubmode = netexVehicleSubmode;
    return this;
  }

  public ZoneId timeZone() {
    return timeZone;
  }

  public StopBuilder withTimeZone(ZoneId timeZone) {
    this.timeZone = timeZone;
    return this;
  }

  public StopBuilder addFareZones(FareZone fareZone) {
    this.fareZones.add(fareZone);
    return this;
  }

  public Set<FareZone> fareZones() {
    return fareZones;
  }

  public StopBuilder addBoardingArea(BoardingArea boardingArea) {
    boardingAreas.add(boardingArea);
    return this;
  }

  public Collection<BoardingArea> boardingAreas() {
    return boardingAreas;
  }

  @Override
  StopBuilder instance() {
    return this;
  }

  @Override
  protected Stop buildFromValues() {
    return new Stop(this);
  }
}
