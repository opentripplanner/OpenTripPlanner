package org.opentripplanner.transit.model.site;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class GroupOfStationsBuilder
  extends AbstractEntityBuilder<GroupOfStations, GroupOfStationsBuilder> {

  private I18NString name;
  private Set<StopLocationsGroup> childStations = new HashSet<>();
  private GroupOfStationsPurpose purposeOfGrouping;
  private WgsCoordinate coordinate;

  GroupOfStationsBuilder(FeedScopedId id) {
    super(id);
  }

  GroupOfStationsBuilder(@Nonnull GroupOfStations original) {
    super(original);
    // Required fields
    this.name = I18NString.assertHasValue(original.getName());
    this.childStations = new HashSet<>(original.getChildStations());
    // Optional fields
    this.purposeOfGrouping = original.getPurposeOfGrouping();
    this.coordinate = original.getCoordinate();
  }

  @Override
  protected GroupOfStations buildFromValues() {
    return new GroupOfStations(this);
  }

  public GroupOfStationsBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public I18NString name() {
    return name;
  }

  public GroupOfStationsBuilder addChildStation(StopLocationsGroup station) {
    this.childStations.add(station);
    return this;
  }

  public Set<StopLocationsGroup> childStations() {
    return Set.copyOf(this.childStations);
  }

  public GroupOfStationsBuilder withCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
    return this;
  }

  public WgsCoordinate coordinate() {
    return coordinate;
  }

  public GroupOfStationsBuilder withPurposeOfGrouping(GroupOfStationsPurpose purposeOfGrouping) {
    this.purposeOfGrouping = purposeOfGrouping;
    return this;
  }

  public GroupOfStationsPurpose purposeOfGrouping() {
    return purposeOfGrouping;
  }
}
