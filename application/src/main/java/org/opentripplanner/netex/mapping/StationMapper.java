package org.opentripplanner.netex.mapping;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.support.JAXBUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.rutebanken.netex.model.LimitedUseTypeEnumeration;
import org.rutebanken.netex.model.LocaleStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.NameTypeEnumeration;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.Zone_VersionStructure;

class StationMapper {

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  private final ZoneId defaultTimeZone;

  private final boolean noTransfersOnIsolatedStops;

  private final Set<FeedScopedId> routeToCentroidStopPlaceIds;

  private final SiteRepositoryBuilder siteRepositoryBuilder;

  StationMapper(
    DataImportIssueStore issueStore,
    FeedScopedIdFactory idFactory,
    ZoneId defaultTimeZone,
    boolean noTransfersOnIsolatedStops,
    Set<FeedScopedId> routeToCentroidStopPlaceIds,
    SiteRepositoryBuilder siteRepositoryBuilder
  ) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
    this.defaultTimeZone = defaultTimeZone;
    this.noTransfersOnIsolatedStops = noTransfersOnIsolatedStops;
    this.routeToCentroidStopPlaceIds = routeToCentroidStopPlaceIds;
    this.siteRepositoryBuilder = siteRepositoryBuilder;
  }

  Station map(StopPlace stopPlace) {
    var id = idFactory.createId(stopPlace.getId());
    return siteRepositoryBuilder.computeStationIfAbsent(id, it ->
      mapStopPlaceToStation(it, stopPlace)
    );
  }

  Station mapStopPlaceToStation(FeedScopedId id, StopPlace stopPlace) {
    var builder = Station.of(id)
      .withName(resolveName(stopPlace))
      .withCoordinate(mapCoordinate(stopPlace))
      .withShouldRouteToCentroid(shouldRouteToCentroid(id))
      .withDescription(
        NonLocalizedString.ofNullable(stopPlace.getDescription(), MultilingualString::getValue)
      )
      .withPriority(StopTransferPriorityMapper.mapToDomain(stopPlace.getWeighting()))
      .withTimezone(
        Optional.ofNullable(stopPlace.getLocale())
          .map(LocaleStructure::getTimeZone)
          .map(zoneId -> ofZoneId(stopPlace.getId(), zoneId))
          .orElse(defaultTimeZone)
      );

    if (noTransfersOnIsolatedStops) {
      builder.withTransfersNotAllowed(
        LimitedUseTypeEnumeration.ISOLATED.equals(stopPlace.getLimitedUse())
      );
    }

    return builder.build();
  }

  private boolean shouldRouteToCentroid(FeedScopedId stopPlaceId) {
    return routeToCentroidStopPlaceIds.contains(stopPlaceId);
  }

  private ZoneId ofZoneId(String stopPlaceId, String zoneId) {
    try {
      return ZoneId.of(zoneId);
    } catch (DateTimeException e) {
      issueStore.add(
        "InvalidTimeZone",
        "Invalid ID for ZoneOffset at StopPlace with ID: %s and value %s",
        stopPlaceId,
        zoneId
      );
    }
    return null;
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
   * Map the centroid to coordinate, if not present the mean coordinate for the child quays is
   * returned. If the station do not have any quays an exception is thrown.
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
      List<WgsCoordinate> coordinates = JAXBUtils.streamJAXBElementValue(
        Quay.class,
        stopPlace.getQuays().getQuayRefOrQuay()
      )
        .map(Zone_VersionStructure::getCentroid)
        .filter(Objects::nonNull)
        .map(WgsCoordinateMapper::mapToDomain)
        .toList();
      if (coordinates.isEmpty()) {
        throw new IllegalArgumentException(
          "Station w/quays without coordinates. Station id: " + stopPlace.getId()
        );
      }
      return WgsCoordinate.mean(coordinates);
    }
  }
}
