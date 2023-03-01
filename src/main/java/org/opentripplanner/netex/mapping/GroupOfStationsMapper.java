package org.opentripplanner.netex.mapping;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.GroupOfStationsBuilder;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.StopPlaceRefStructure;
import org.rutebanken.netex.model.StopPlaceRefs_RelStructure;

class GroupOfStationsMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  private final EntityById<MultiModalStation> multiModalStations;

  private final EntityById<Station> stations;

  GroupOfStationsMapper(
    DataImportIssueStore issueStore,
    FeedScopedIdFactory idFactory,
    EntityById<MultiModalStation> multiModalStations,
    EntityById<Station> stations
  ) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
    this.multiModalStations = multiModalStations;
    this.stations = stations;
  }

  GroupOfStations map(GroupOfStopPlaces groupOfStopPlaces) {
    GroupOfStationsBuilder groupOfStations = GroupOfStations
      .of(idFactory.createId(groupOfStopPlaces.getId()))
      .withName(NonLocalizedString.ofNullable(groupOfStopPlaces.getName().getValue()));

    // TODO Map PurposeOfGrouping from NeTEx

    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(groupOfStopPlaces.getCentroid());

    if (coordinate == null) {
      issueStore.add(
        "GroupOfStationWithoutCoordinates",
        "MultiModal station {} does not contain any coordinates.",
        groupOfStations.getId()
      );
    } else {
      groupOfStations.withCoordinate(coordinate);
    }

    connectChildStation(groupOfStopPlaces, groupOfStations);

    return groupOfStations.build();
  }

  private void connectChildStation(
    GroupOfStopPlaces groupOfStopPlaces,
    GroupOfStationsBuilder groupOfStations
  ) {
    StopPlaceRefs_RelStructure members = groupOfStopPlaces.getMembers();
    if (members != null) {
      List<StopPlaceRefStructure> memberList = members.getStopPlaceRef();
      for (StopPlaceRefStructure stopPlaceRefStructure : memberList) {
        FeedScopedId stationId = idFactory.createId(stopPlaceRefStructure.getRef());
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
