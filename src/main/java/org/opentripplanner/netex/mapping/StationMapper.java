package org.opentripplanner.netex.mapping;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TranslatedString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.site.Station;
import org.rutebanken.netex.model.LimitedUseTypeEnumeration;
import org.rutebanken.netex.model.LocaleStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.NameTypeEnumeration;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

class StationMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  private final ZoneId defaultTimeZone;

  private final boolean noTransfersOnIsolatedStops;

  StationMapper(
    DataImportIssueStore issueStore,
    FeedScopedIdFactory idFactory,
    ZoneId defaultTimeZone,
    boolean noTransfersOnIsolatedStops
  ) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
    this.defaultTimeZone = defaultTimeZone;
    this.noTransfersOnIsolatedStops = noTransfersOnIsolatedStops;
  }

  Station map(StopPlace stopPlace) {
    var builder = Station
      .of(idFactory.createId(stopPlace.getId()))
      .withName(resolveName(stopPlace))
      .withCoordinate(mapCoordinate(stopPlace))
      .withDescription(
        NonLocalizedString.ofNullable(stopPlace.getDescription(), MultilingualString::getValue)
      )
      .withPriority(StopTransferPriorityMapper.mapToDomain(stopPlace.getWeighting()))
      .withTimezone(
        Optional
          .ofNullable(stopPlace.getLocale())
          .map(LocaleStructure::getTimeZone)
          .map(ZoneId::of)
          .orElse(defaultTimeZone)
      );

    if (noTransfersOnIsolatedStops) {
      builder.withTransfersNotAllowed(
        LimitedUseTypeEnumeration.ISOLATED.equals(stopPlace.getLimitedUse())
      );
    }

    return builder.build();
  }

  private I18NString resolveName(StopPlace stopPlace) {
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
    return name;
  }

  /**
   * Map the centroid to coordinate, if not present the mean coordinate for the
   * child quays is returned. If the station do not have any quays an exception is thrown.
   */
  private WgsCoordinate mapCoordinate(StopPlace stopPlace) {
    if (stopPlace.getCentroid() != null) {
      return WgsCoordinateMapper.mapToDomain(stopPlace.getCentroid());
    } else {
      issueStore.add(
        "StationWithoutCoordinates",
        "Station %s does not contain any coordinates.",
        stopPlace.getId() + " " + stopPlace.getName()
      );
      List<WgsCoordinate> coordinates = new ArrayList<>();
      for (Object it : stopPlace.getQuays().getQuayRefOrQuay()) {
        if (it instanceof Quay quay) {
          coordinates.add(WgsCoordinateMapper.mapToDomain(quay.getCentroid()));
        }
      }
      if (coordinates.isEmpty()) {
        throw new IllegalArgumentException(
          "Station w/quays without coordinates. Station id: " + stopPlace.getId()
        );
      }
      return WgsCoordinate.mean(coordinates);
    }
  }
}
