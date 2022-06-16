package org.opentripplanner.transit.model.site;

import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.I18NString;

/**
 * Acts as the supertype for all entities, except stations, created from the GTFS stops table. Most
 * of the fields are shared between the types, and eg. in pathways the namespace any of them can be
 * used as from and to.
 */
public abstract class StationElementBuilder<
  E extends StationElement<E, B>, B extends StationElementBuilder<E, B>
>
  extends AbstractEntityBuilder<E, B> {

  private I18NString name;
  private String code;
  private I18NString description;
  private WgsCoordinate coordinate;
  private WheelchairAccessibility wheelchairAccessibility;
  private StopLevel level;
  private Station parentStation;

  public StationElementBuilder(FeedScopedId id) {
    super(id);
  }

  public StationElementBuilder(E original) {
    super(original);
    this.name = original.getName();
    this.code = original.getCode();
    this.description = original.getDescription();
    this.coordinate = original.getCoordinate();
    this.wheelchairAccessibility = original.getWheelchairAccessibility();
    this.level = original.level();
    this.parentStation = original.getParentStation();
  }

  abstract B instance();

  public I18NString name() {
    return name;
  }

  public B withName(I18NString name) {
    this.name = name;
    return instance();
  }

  public String code() {
    return code;
  }

  public B withCode(String code) {
    this.code = code;
    return instance();
  }

  public I18NString description() {
    return description;
  }

  public B withDescription(I18NString description) {
    this.description = description;
    return instance();
  }

  public WgsCoordinate coordinate() {
    return coordinate;
  }

  public B withCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
    return instance();
  }

  public B withCoordinate(double latitude, double longitude) {
    this.coordinate = new WgsCoordinate(latitude, longitude);
    return instance();
  }

  public WheelchairAccessibility wheelchairAccessibility() {
    return wheelchairAccessibility;
  }

  public B withWheelchairAccessibility(WheelchairAccessibility wheelchairAccessibility) {
    this.wheelchairAccessibility = wheelchairAccessibility;
    return instance();
  }

  public StopLevel level() {
    return level;
  }

  public B withLevel(StopLevel level) {
    this.level = level;
    return instance();
  }

  public Station parentStation() {
    return parentStation;
  }

  public B withParentStation(Station parentStation) {
    this.parentStation = parentStation;
    return instance();
  }
}
