package org.opentripplanner.transit.service;

import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.site.FlexLocationGroup;
import org.opentripplanner.transit.model.site.FlexStopLocation;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;

public class StopModelBuilder {

  private final EntityById<Stop> stopsById = new EntityById<>();
  private final EntityById<Station> stationById = new EntityById<>();
  private final EntityById<MultiModalStation> multiModalStationById = new EntityById<>();
  private final EntityById<GroupOfStations> groupOfStationsById = new EntityById<>();
  private final EntityById<FlexStopLocation> flexStopsById = new EntityById<>();
  private final EntityById<FlexLocationGroup> flexStopGroupsById = new EntityById<>();

  StopModelBuilder() {}

  StopModelBuilder(StopModel stopModel) {
    stopsById.addAll(stopModel.getAllStops());
    stationById.addAll(stopModel.getStations());
    multiModalStationById.addAll(stopModel.getAllMultiModalStations());
    groupOfStationsById.addAll(stopModel.getAllGroupOfStations());
    flexStopsById.addAll(stopModel.getAllFlexLocations());
    flexStopGroupsById.addAll(stopModel.getAllFlexStopGroups());
  }

  public EntityById<Stop> stopsById() {
    return stopsById;
  }

  public void addStop(Stop stop) {
    stopsById.add(stop);
  }

  public EntityById<Station> stationsById() {
    return stationById;
  }

  public void addStation(Station station) {
    stationById.add(station);
  }

  public EntityById<MultiModalStation> multiModalStationsById() {
    return multiModalStationById;
  }

  public void addMultiModalStation(MultiModalStation station) {
    multiModalStationById.add(station);
  }

  public EntityById<GroupOfStations> groupOfStationsById() {
    return groupOfStationsById;
  }

  public void addGroupOfStation(GroupOfStations station) {
    groupOfStationsById.add(station);
  }

  public EntityById<FlexStopLocation> flexStopsById() {
    return flexStopsById;
  }

  public void addFlexStop(FlexStopLocation stop) {
    flexStopsById.add(stop);
  }

  public EntityById<FlexLocationGroup> flexStopGroupsById() {
    return flexStopGroupsById;
  }

  public void addFlexStopGroup(FlexLocationGroup group) {
    flexStopGroupsById.add(group);
  }

  public StopModel build() {
    return new StopModel(this);
  }
}
