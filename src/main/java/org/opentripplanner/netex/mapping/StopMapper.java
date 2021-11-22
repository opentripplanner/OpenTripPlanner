package org.opentripplanner.netex.mapping;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.netex.issues.QuayWithoutCoordinates;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

class StopMapper {

  private final StopPlaceTypeMapper stopPlaceTypeMapper = new StopPlaceTypeMapper();

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  StopMapper(
      FeedScopedIdFactory idFactory,
      DataImportIssueStore issueStore
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
  }

  /**
   * Map Netex Quay to OTP Stop
   */
  @Nullable
  Stop mapQuayToStop(
          Quay quay,
          Station parentStation,
          Collection<FareZone> fareZones,
          StopPlace stopPlace
  ) {
    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(quay.getCentroid());

    if (coordinate == null) {
      issueStore.add(new QuayWithoutCoordinates(quay.getId()));
      return null;
    }

    Stop stop = new Stop(
        idFactory.createId(quay.getId()),
        parentStation.getName(),
        quay.getPublicCode(),
        quay.getDescription() != null ? quay.getDescription().getValue() : null,
        WgsCoordinateMapper.mapToDomain(quay.getCentroid()),
        null,
        null,
        null,
        fareZones,
        null,
        null,
        TransitModeMapper.mapMode(stopPlaceTypeMapper.getTransportMode(stopPlace))
    );
    stop.setParentStation(parentStation);

    return stop;
  }
}
