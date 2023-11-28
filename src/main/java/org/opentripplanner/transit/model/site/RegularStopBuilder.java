/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or a
 * platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class RegularStopBuilder
  extends StationElementBuilder<RegularStop, RegularStopBuilder> {

  private final IntSupplier indexCounter;

  private String platformCode;

  private I18NString url;

  private ZoneId timeZone;

  private TransitMode gtfsVehicleType;

  private String netexVehicleSubmode;

  private final Set<BoardingArea> boardingAreas = new HashSet<>();

  private final Set<FareZone> fareZones = new HashSet<>();

  RegularStopBuilder(FeedScopedId id, IntSupplier indexCounter) {
    super(id);
    this.indexCounter = Objects.requireNonNull(indexCounter);
  }

  RegularStopBuilder(RegularStop original) {
    super(original);
    this.indexCounter = original::getIndex;
    this.platformCode = original.getPlatformCode();
    this.url = original.getUrl();
    this.timeZone = original.getTimeZone();
    this.gtfsVehicleType = original.getGtfsVehicleType();
    this.netexVehicleSubmode = original.getNetexVehicleSubmode().name();
  }

  public String platformCode() {
    return platformCode;
  }

  public RegularStopBuilder withPlatformCode(String platformCode) {
    this.platformCode = platformCode;
    return this;
  }

  public I18NString url() {
    return url;
  }

  public RegularStopBuilder withUrl(I18NString url) {
    this.url = url;
    return this;
  }

  public TransitMode vehicleType() {
    return gtfsVehicleType;
  }

  public RegularStopBuilder withVehicleType(TransitMode vehicleType) {
    this.gtfsVehicleType = vehicleType;
    return this;
  }

  public String netexVehicleSubmode() {
    return netexVehicleSubmode;
  }

  public RegularStopBuilder withNetexVehicleSubmode(String netexVehicleSubmode) {
    this.netexVehicleSubmode = netexVehicleSubmode;
    return this;
  }

  public ZoneId timeZone() {
    return timeZone;
  }

  public RegularStopBuilder withTimeZone(ZoneId timeZone) {
    this.timeZone = timeZone;
    return this;
  }

  public RegularStopBuilder addFareZones(FareZone fareZone) {
    this.fareZones.add(fareZone);
    return this;
  }

  public Set<FareZone> fareZones() {
    return fareZones;
  }

  public RegularStopBuilder addBoardingArea(BoardingArea boardingArea) {
    boardingAreas.add(boardingArea);
    return this;
  }

  public Collection<BoardingArea> boardingAreas() {
    return boardingAreas;
  }

  @Override
  RegularStopBuilder instance() {
    return this;
  }

  @Override
  protected RegularStop buildFromValues() {
    return new RegularStop(this);
  }

  int createIndex() {
    return indexCounter.getAsInt();
  }
}
