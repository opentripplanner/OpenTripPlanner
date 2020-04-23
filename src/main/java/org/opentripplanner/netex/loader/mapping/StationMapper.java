package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.Station;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(StationMapper.class);

  private final FeedScopedIdFactory idFactory;

  StationMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Station map(StopPlace stopPlace) {
    Station station = new Station(
        idFactory.createId(stopPlace.getId()),
        stopPlace.getName() == null ? "N/A" : stopPlace.getName().getValue(),
        WgsCoordinateMapper.mapToDomain(stopPlace.getCentroid()),
        null,
        null,
        null,
        null,
        TransferPriorityMapper.mapToDomain(stopPlace.getWeighting())
    );

    if (station.getCoordinate() == null) {
      LOG.warn("Station {} does not contain any coordinates.", station.getId());
    }
    return station;
  }
}
