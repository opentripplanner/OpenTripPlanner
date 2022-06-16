package org.opentripplanner.transit.model.site;

import java.util.TimeZone;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.I18NString;

public class StationBuilder extends AbstractEntityBuilder<Station, StationBuilder> {

  private I18NString name;
  private String code;
  private I18NString description;
  private WgsCoordinate coordinate;
  private StopTransferPriority priority;
  private I18NString url;
  private TimeZone timezone;

  StationBuilder(FeedScopedId id) {
    super(id);
  }

  StationBuilder(Station original) {
    super(original);
    this.name = original.getName();
    this.code = original.getCode();
    this.description = original.getDescription();
    this.coordinate = original.getCoordinate();
    this.priority = original.getPriority();
    this.url = original.getUrl();
    this.timezone = original.getTimezone();
  }

  public I18NString getName() {
    return name;
  }

  public StationBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public String getCode() {
    return code;
  }

  public StationBuilder withCode(String code) {
    this.code = code;
    return this;
  }

  public I18NString getDescription() {
    return description;
  }

  public StationBuilder withDescription(I18NString description) {
    this.description = description;
    return this;
  }

  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  public StationBuilder withCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
    return this;
  }

  public StationBuilder withCoordinate(double latitude, double longitude) {
    this.coordinate = new WgsCoordinate(latitude, longitude);
    return this;
  }

  public StopTransferPriority getPriority() {
    return priority;
  }

  public StationBuilder withPriority(StopTransferPriority priority) {
    this.priority = priority;
    return this;
  }

  public I18NString getUrl() {
    return url;
  }

  public StationBuilder withUrl(I18NString url) {
    this.url = url;
    return this;
  }

  public TimeZone getTimezone() {
    return timezone;
  }

  public StationBuilder withTimezone(TimeZone timezone) {
    this.timezone = timezone;
    return this;
  }

  @Override
  protected Station buildFromValues() {
    return new Station(this);
  }
}
