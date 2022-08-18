package org.opentripplanner.transit.service;

import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.site.FlexLocationGroup;
import org.opentripplanner.transit.model.site.FlexStopLocation;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.util.MedianCalcForDoubles;
import org.opentripplanner.util.lang.CollectionsView;

public class StopModelBuilder {

  private final EntityById<RegularStop> stopsById = new EntityById<>();
  private final EntityById<Station> stationById = new EntityById<>();
  private final EntityById<MultiModalStation> multiModalStationById = new EntityById<>();
  private final EntityById<GroupOfStations> groupOfStationsById = new EntityById<>();
  private final EntityById<FlexStopLocation> flexStopsById = new EntityById<>();
  private final EntityById<FlexLocationGroup> flexStopGroupsById = new EntityById<>();

  StopModelBuilder() {}

  StopModelBuilder(StopModel stopModel) {
    addAll(stopModel);
  }

  public EntityById<RegularStop> stopsById() {
    return stopsById;
  }

  public StopModelBuilder withStop(RegularStop stop) {
    stopsById.add(stop);
    return this;
  }

  public EntityById<Station> stationsById() {
    return stationById;
  }

  public StopModelBuilder withStation(Station station) {
    stationById.add(station);
    return this;
  }

  public EntityById<MultiModalStation> multiModalStationsById() {
    return multiModalStationById;
  }

  public StopModelBuilder withMultiModalStation(MultiModalStation station) {
    multiModalStationById.add(station);
    return this;
  }

  public EntityById<GroupOfStations> groupOfStationsById() {
    return groupOfStationsById;
  }

  public StopModelBuilder withGroupOfStation(GroupOfStations station) {
    groupOfStationsById.add(station);
    return this;
  }

  public EntityById<FlexStopLocation> flexStopsById() {
    return flexStopsById;
  }

  public StopModelBuilder withFlexStop(FlexStopLocation stop) {
    flexStopsById.add(stop);
    return this;
  }

  public EntityById<FlexLocationGroup> flexStopGroupsById() {
    return flexStopGroupsById;
  }

  public StopModelBuilder withFlexStopGroup(FlexLocationGroup group) {
    flexStopGroupsById.add(group);
    return this;
  }

  /**
   * Add the content of another stop model. There are no collision check, entities in the given
   * {@code other} model, will replace existing entities.
   */
  public StopModelBuilder addAll(StopModel other) {
    stopsById.addAll(other.listRegularStops());
    stationById.addAll(other.getStations());
    multiModalStationById.addAll(other.getAllMultiModalStations());
    groupOfStationsById.addAll(other.getAllGroupOfStations());
    flexStopsById.addAll(other.getAllFlexLocations());
    flexStopGroupsById.addAll(other.getAllFlexStopGroups());
    return this;
  }

  /**
   * Calculates Transit center from median of coordinates of all transitStops if graph has transit.
   * If it doesn't it isn't calculated. (mean value of min, max latitude and longitudes are used)
   * <p>
   * Transit center is saved in center variable
   * <p>
   * This speeds up calculation, but problem is that median needs to have all of
   * latitudes/longitudes in memory, this can become problematic in large installations. It works
   * without a problem on New York State.
   */
  WgsCoordinate calculateTransitCenter() {
    var stops = new CollectionsView<>(
      stopsById.values(),
      flexStopsById.values(),
      flexStopGroupsById.values()
    );

    if (stops.isEmpty()) {
      return null;
    }

    // we need this check because there could be only FlexStopLocations (which don't have vertices)
    // in the graph
    var medianCalculator = new MedianCalcForDoubles(stops.size());

    stops.forEach(v -> medianCalculator.add(v.getLon()));
    double lon = medianCalculator.median();

    medianCalculator.reset();
    stops.forEach(v -> medianCalculator.add(v.getLat()));
    double lat = medianCalculator.median();

    return new WgsCoordinate(lat, lon);
  }

  public StopModel build() {
    return new StopModel(this);
  }
}
