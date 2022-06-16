package org.opentripplanner.netex.mapping;

import java.util.Collection;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.util.NonLocalizedString;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.StopPlace;

class MultiModalStationMapper {

  private final DataImportIssueStore issueStore;
  private final FeedScopedIdFactory idFactory;

  public MultiModalStationMapper(DataImportIssueStore issueStore, FeedScopedIdFactory idFactory) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
  }

  MultiModalStation map(StopPlace stopPlace, Collection<Station> childStations) {
    MultiModalStation multiModalStation = new MultiModalStation(
      idFactory.createId(stopPlace.getId()),
      childStations
    );

    multiModalStation.setName(
      NonLocalizedString.ofNullable(stopPlace.getName(), MultilingualString::getValue, "N/A")
    );

    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(stopPlace.getCentroid());

    if (coordinate == null) {
      issueStore.add(
        "MultiModalStationWithoutCoordinates",
        "MultiModal station {} does not contain any coordinates.",
        multiModalStation.getId()
      );
    } else {
      multiModalStation.setCoordinate(coordinate);
    }
    return multiModalStation;
  }
}
