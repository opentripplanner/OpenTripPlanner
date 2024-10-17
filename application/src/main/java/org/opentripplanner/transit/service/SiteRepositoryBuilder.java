package org.opentripplanner.transit.service;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.ImmutableEntityById;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.AreaStopBuilder;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.GroupStopBuilder;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;

public class SiteRepositoryBuilder {

  private final AtomicInteger stopIndexCounter;

  private final EntityById<RegularStop> regularStopById = new DefaultEntityById<>();
  private final EntityById<AreaStop> areaStopById = new DefaultEntityById<>();
  private final EntityById<GroupStop> groupStopById = new DefaultEntityById<>();
  private final EntityById<Station> stationById = new DefaultEntityById<>();
  private final EntityById<MultiModalStation> multiModalStationById = new DefaultEntityById<>();
  private final EntityById<GroupOfStations> groupOfStationById = new DefaultEntityById<>();

  SiteRepositoryBuilder(AtomicInteger stopIndexCounter) {
    this.stopIndexCounter = stopIndexCounter;
  }

  public ImmutableEntityById<RegularStop> regularStopsById() {
    return regularStopById;
  }

  public RegularStopBuilder regularStop(FeedScopedId id) {
    return RegularStop.of(id, stopIndexCounter::getAndIncrement);
  }

  public RegularStop computeRegularStopIfAbsent(
    FeedScopedId id,
    Function<FeedScopedId, RegularStop> factory
  ) {
    return regularStopById.computeIfAbsent(id, factory);
  }

  public SiteRepositoryBuilder withRegularStop(RegularStop stop) {
    regularStopById.add(stop);
    return this;
  }

  public SiteRepositoryBuilder withRegularStops(Collection<RegularStop> stops) {
    regularStopById.addAll(stops);
    return this;
  }

  public ImmutableEntityById<Station> stationById() {
    return stationById;
  }

  public SiteRepositoryBuilder withStation(Station station) {
    stationById.add(station);
    return this;
  }

  public Station computeStationIfAbsent(FeedScopedId id, Function<FeedScopedId, Station> body) {
    return stationById.computeIfAbsent(id, body::apply);
  }

  public SiteRepositoryBuilder withStations(Collection<Station> stations) {
    stationById.addAll(stations);
    return this;
  }

  public ImmutableEntityById<MultiModalStation> multiModalStationById() {
    return multiModalStationById;
  }

  public SiteRepositoryBuilder withMultiModalStation(MultiModalStation station) {
    multiModalStationById.add(station);
    return this;
  }

  public ImmutableEntityById<GroupOfStations> groupOfStationById() {
    return groupOfStationById;
  }

  public SiteRepositoryBuilder withGroupOfStation(GroupOfStations station) {
    groupOfStationById.add(station);
    return this;
  }

  public AreaStopBuilder areaStop(FeedScopedId id) {
    return AreaStop.of(id, stopIndexCounter::getAndIncrement);
  }

  public ImmutableEntityById<AreaStop> areaStopById() {
    return areaStopById;
  }

  public SiteRepositoryBuilder withAreaStop(AreaStop stop) {
    areaStopById.add(stop);
    return this;
  }

  public SiteRepositoryBuilder withAreaStops(Collection<AreaStop> stops) {
    areaStopById.addAll(stops);
    return this;
  }

  public GroupStopBuilder groupStop(FeedScopedId id) {
    return GroupStop.of(id, stopIndexCounter::getAndIncrement);
  }

  public ImmutableEntityById<GroupStop> groupStopById() {
    return groupStopById;
  }

  public SiteRepositoryBuilder withGroupStop(GroupStop group) {
    groupStopById.add(group);
    return this;
  }

  public SiteRepositoryBuilder withGroupStops(Collection<GroupStop> groups) {
    groupStopById.addAll(groups);
    return this;
  }

  /**
   * Add the content of another site repository. There are no collision check, entities in the given
   * {@code other} model, will replace existing entities.
   */
  public SiteRepositoryBuilder addAll(SiteRepository other) {
    regularStopById.addAll(other.listRegularStops());
    stationById.addAll(other.listStations());
    multiModalStationById.addAll(other.listMultiModalStations());
    groupOfStationById.addAll(other.listGroupOfStations());
    areaStopById.addAll(other.listAreaStops());
    groupStopById.addAll(other.listGroupStops());
    return this;
  }

  public SiteRepository build() {
    return new SiteRepository(this);
  }

  AtomicInteger stopIndexCounter() {
    return stopIndexCounter;
  }
}
