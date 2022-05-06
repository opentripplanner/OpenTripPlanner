package org.opentripplanner.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.util.I18NString;

/**
 * A grouping that can contain a mix of Stations and MultiModalStations. It can be used to link
 * several StopPlaces into a hub. It can be a grouping of major stops within a city or a cluster of
 * stops that naturally belong together.
 */
public class GroupOfStations extends TransitEntity implements StopCollection {

  private static final long serialVersionUID = 1L;
  private final Set<StopCollection> childStations = new HashSet<>();
  private I18NString name;
  // TODO Map from NeTEx
  private PurposeOfGrouping purposeOfGrouping;
  private WgsCoordinate coordinate;

  public GroupOfStations(FeedScopedId id) {
    super(id);
  }

  public I18NString getName() {
    return name;
  }

  public void setName(I18NString name) {
    this.name = name;
  }

  public Collection<StopLocation> getChildStops() {
    return this.childStations.stream()
      .flatMap(s -> s.getChildStops().stream())
      .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  public void setCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
  }

  public Collection<StopCollection> getChildStations() {
    return this.childStations;
  }

  public void addChildStation(StopCollection station) {
    this.childStations.add(station);
  }

  /**
   * Categorization for the grouping
   */
  public PurposeOfGrouping getPurposeOfGrouping() {
    return purposeOfGrouping;
  }

  public void setPurposeOfGrouping(PurposeOfGrouping purposeOfGrouping) {
    this.purposeOfGrouping = purposeOfGrouping;
  }

  @Override
  public String toString() {
    return "<GroupOfStations " + getId() + ">";
  }

  /**
   * Categorization for the grouping
   */
  public enum PurposeOfGrouping {
    /**
     * Group of prominent stop places within a town or city(centre)
     */
    GENERALIZATION,
    /**
     * Stop places in proximity to each other which have a natural geospatial- or public transport
     * related relationship.
     */
    CLUSTER,
  }
}
