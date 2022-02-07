package org.opentripplanner.netex.mapping;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Station;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.StopPlace;


class StationMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  StationMapper(DataImportIssueStore issueStore, FeedScopedIdFactory idFactory) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
  }

  Station map(StopPlace stopPlace) {
    Station station = new Station(
        idFactory.createId(stopPlace.getId()),
        stopPlace.getName() == null ? "N/A" : stopPlace.getName().getValue(),
        WgsCoordinateMapper.mapToDomain(stopPlace.getCentroid()),
        null,
        stopPlace.getDescription() != null ? stopPlace.getDescription().getValue() : null,
        null,
        null,
        StopTransferPriorityMapper.mapToDomain(stopPlace.getWeighting())
    );

    if (station.getCoordinate() == null) {
      issueStore.add(
              "StationWithoutCoordinates",
              "Station %s does not contain any coordinates.",
              station.getId()
      );
    }
    return station;
  }
}
