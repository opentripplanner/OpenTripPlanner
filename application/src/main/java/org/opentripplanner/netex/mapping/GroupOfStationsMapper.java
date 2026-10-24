package org.opentripplanner.netex.mapping;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.WgsCoordinateMapper.InvalidCoordinateException;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.ImmutableEntityById;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.GroupOfStationsBuilder;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.StopPlaceRefs_RelStructure;

class GroupOfStationsMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  private final ImmutableEntityById<MultiModalStation> multiModalStations;

  private final ImmutableEntityById<Station> stations;

  GroupOfStationsMapper(
    DataImportIssueStore issueStore,
    FeedScopedIdFactory idFactory,
    ImmutableEntityById<MultiModalStation> multiModalStations,
    ImmutableEntityById<Station> stations
  ) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
    this.multiModalStations = multiModalStations;
    this.stations = stations;
  }

  GroupOfStations map(GroupOfStopPlaces groupOfStopPlaces) {
    GroupOfStationsBuilder groupOfStations = GroupOfStations.of(
      idFactory.createId(groupOfStopPlaces.getId())
    ).withName(mapName(groupOfStopPlaces));

    // TODO Map PurposeOfGrouping from NeTEx

    WgsCoordinate coordinate = mapCoordinate(groupOfStopPlaces, groupOfStations.getId());
    if (coordinate != null) {
      groupOfStations.withCoordinate(coordinate);
    }

    connectChildStation(groupOfStopPlaces, groupOfStations);

    return groupOfStations.build();
  }

  /**
   * The name is required. If it is missing, the name of the first member found is used instead. A
   * member may be missing, e.g. a StopPlace without valid coordinates is dropped, so fall back to
   * "N/A" if no member is found.
   */
  private I18NString mapName(GroupOfStopPlaces groupOfStopPlaces) {
    MultilingualString name = groupOfStopPlaces.getName();
    if (name != null && name.getValue() != null) {
      return new NonLocalizedString(name.getValue());
    }
    issueStore.add(
      "GroupOfStopPlacesWithoutName",
      "GroupOfStopPlaces %s does not contain a name.",
      groupOfStopPlaces.getId()
    );
    return memberIds(groupOfStopPlaces)
      .stream()
      .map(this::lookupStation)
      .filter(Objects::nonNull)
      .map(StopLocationsGroup::getName)
      .findFirst()
      .orElse(new NonLocalizedString("N/A"));
  }

  /**
   * Map the centroid to a coordinate. The coordinate is optional for a GroupOfStations, so a
   * missing or invalid centroid is reported as an issue and {@code null} is returned.
   */
  @Nullable
  private WgsCoordinate mapCoordinate(GroupOfStopPlaces groupOfStopPlaces, FeedScopedId id) {
    try {
      WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(groupOfStopPlaces.getCentroid());
      if (coordinate == null) {
        issueStore.add(
          "GroupOfStationWithoutCoordinates",
          "GroupOfStations %s does not contain any coordinates.",
          id
        );
      }
      return coordinate;
    } catch (InvalidCoordinateException e) {
      issueStore.add(
        "GroupOfStationInvalidCoordinates",
        "GroupOfStations %s has invalid coordinates, the coordinates are ignored. %s",
        id,
        e.getMessage()
      );
      return null;
    }
  }

  private void connectChildStation(
    GroupOfStopPlaces groupOfStopPlaces,
    GroupOfStationsBuilder groupOfStations
  ) {
    for (FeedScopedId stationId : memberIds(groupOfStopPlaces)) {
      StopLocationsGroup station = lookupStation(stationId);
      if (station != null) {
        groupOfStations.addChildStation(station);
      } else {
        issueStore.add(
          "GroupOfStationWithoutStations",
          "GroupOfStation %s child not found: %s",
          groupOfStations.getId(),
          stationId
        );
      }
    }
  }

  private List<FeedScopedId> memberIds(GroupOfStopPlaces groupOfStopPlaces) {
    StopPlaceRefs_RelStructure members = groupOfStopPlaces.getMembers();
    if (members == null) {
      return List.of();
    }
    return members
      .getStopPlaceRef()
      .stream()
      .map(ref -> idFactory.createId(ref.getValue().getRef()))
      .toList();
  }

  @Nullable
  private StopLocationsGroup lookupStation(FeedScopedId stationId) {
    if (stations.containsKey(stationId)) {
      return stations.get(stationId);
    } else if (multiModalStations.containsKey(stationId)) {
      return multiModalStations.get(stationId);
    }
    return null;
  }
}
