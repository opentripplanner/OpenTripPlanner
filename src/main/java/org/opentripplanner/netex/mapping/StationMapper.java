package org.opentripplanner.netex.mapping;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Station;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;
import org.rutebanken.netex.model.NameTypeEnumeration;
import org.rutebanken.netex.model.StopPlace;

class StationMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  StationMapper(DataImportIssueStore issueStore, FeedScopedIdFactory idFactory) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
  }

  Station map(StopPlace stopPlace) {
    final I18NString name;
    if (stopPlace.getName() == null) {
      name = new NonLocalizedString("N/A");
    } else if (stopPlace.getAlternativeNames() != null) {
      Map<String, String> translations = new HashMap<>();
      translations.put(null, stopPlace.getName().getValue());
      for (var translation : stopPlace.getAlternativeNames().getAlternativeName()) {
        if (translation.getNameType() == NameTypeEnumeration.TRANSLATION) {
          String lang = translation.getLang() != null
            ? translation.getLang()
            : translation.getName().getLang();
          translations.put(lang, translation.getName().getValue());
        }
      }

      name = TranslatedString.getI18NString(translations, true, false);
    } else {
      name = new NonLocalizedString(stopPlace.getName().getValue());
    }

    Station station = new Station(
      idFactory.createId(stopPlace.getId()),
      name,
      WgsCoordinateMapper.mapToDomain(stopPlace.getCentroid()),
      null,
      stopPlace.getDescription() != null
        ? new NonLocalizedString(stopPlace.getDescription().getValue())
        : null,
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
