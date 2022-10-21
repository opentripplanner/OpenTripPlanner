package org.opentripplanner.transit.service;

import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.util.MedianCalcForDoubles;
import org.opentripplanner.util.lang.CollectionsView;

public class StopModelBuilder {

  private final EntityById<RegularStop> regularStopById = new EntityById<>();
  private final EntityById<AreaStop> areaStopById = new EntityById<>();
  private final EntityById<GroupStop> groupStopById = new EntityById<>();
  private final EntityById<Station> stationById = new EntityById<>();
  private final EntityById<MultiModalStation> multiModalStationById = new EntityById<>();
  private final EntityById<GroupOfStations> groupOfStationById = new EntityById<>();

  StopModelBuilder() {}

  StopModelBuilder(StopModel stopModel) {
    addAll(stopModel);
  }

  public EntityById<RegularStop> regularStopsById() {
    return regularStopById;
  }

  public StopModelBuilder withRegularStop(RegularStop stop) {
    regularStopById.add(stop);
    return this;
  }

  public EntityById<Station> stationById() {
    return stationById;
  }

  public StopModelBuilder withStation(Station station) {
    stationById.add(station);
    return this;
  }

  public EntityById<MultiModalStation> multiModalStationById() {
    return multiModalStationById;
  }

  public StopModelBuilder withMultiModalStation(MultiModalStation station) {
    multiModalStationById.add(station);
    return this;
  }

  public EntityById<GroupOfStations> groupOfStationById() {
    return groupOfStationById;
  }

  public StopModelBuilder withGroupOfStation(GroupOfStations station) {
    groupOfStationById.add(station);
    return this;
  }

  public EntityById<AreaStop> areaStopById() {
    return areaStopById;
  }

  public StopModelBuilder withAreaStop(AreaStop stop) {
    areaStopById.add(stop);
    return this;
  }

  public EntityById<GroupStop> groupStopById() {
    return groupStopById;
  }

  public StopModelBuilder withGroupStop(GroupStop group) {
    groupStopById.add(group);
    return this;
  }

  /**
   * Add the content of another stop model. There are no collision check, entities in the given
   * {@code other} model, will replace existing entities.
   */
  public StopModelBuilder addAll(StopModel other) {
    regularStopById.addAll(other.listRegularStops());
    stationById.addAll(other.listStations());
    multiModalStationById.addAll(other.listMultiModalStations());
    groupOfStationById.addAll(other.listGroupOfStations());
    areaStopById.addAll(other.listAreaStops());
    groupStopById.addAll(other.listGroupStops());
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
      regularStopById.values(),
      areaStopById.values(),
      groupStopById.values()
    );

    if (stops.isEmpty()) {
      return null;
    }

    // we need this check because there could be only AreaStops (which don't have vertices)
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
