package org.opentripplanner.netex.mapping;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.MultiModalStationBuilder;
import org.opentripplanner.transit.model.site.Station;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.StopPlace;

class MultiModalStationMapper {

  private final DataImportIssueStore issueStore;
  private final FeedScopedIdFactory idFactory;

  public MultiModalStationMapper(DataImportIssueStore issueStore, FeedScopedIdFactory idFactory) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
  }

  @Nullable
  MultiModalStation map(StopPlace stopPlace, Collection<Station> childStations) {
    MultiModalStationBuilder multiModalStation = MultiModalStation.of(
      idFactory.createId(stopPlace.getId())
    )
      .withChildStations(childStations)
      .withName(
        NonLocalizedString.ofNullable(stopPlace.getName(), MultilingualString::getValue, "N/A")
      );

    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(stopPlace.getCentroid());

    if (coordinate == null) {
      issueStore.add(
        "MultiModalStationWithoutCoordinates",
        "MultiModal station %s does not contain any coordinates.",
        multiModalStation.getId()
      );
      return null;
    } else {
      multiModalStation.withCoordinate(coordinate);
      return multiModalStation.build();
    }
  }
}
