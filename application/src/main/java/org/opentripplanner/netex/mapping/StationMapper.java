package org.opentripplanner.netex.mapping;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.i18n.TranslatedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.WgsCoordinateMapper.InvalidCoordinateException;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.support.JAXBUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
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

  /**
   * Map a NeTEx StopPlace to a Station.
   *
   * @return {@code null} if the StopPlace cannot be mapped because it has no valid coordinates,
   *         neither on the StopPlace itself nor on any of its quays. A data import issue is
   *         reported in that case.
   */
  @Nullable
  Station map(StopPlace stopPlace) {
    var id = idFactory.createId(stopPlace.getId());
    return siteRepositoryBuilder.computeStationIfAbsent(id, it ->
      mapStopPlaceToStation(it, stopPlace)
    );
  }

  @Nullable
  Station mapStopPlaceToStation(FeedScopedId id, StopPlace stopPlace) {
    WgsCoordinate coordinate = mapCoordinate(stopPlace);
    if (coordinate == null) {
      return null;
    }

    var builder = Station.of(id)
      .withName(resolveName(stopPlace))
      .withCoordinate(coordinate)
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
          String lang =
            translation.getLang() != null ? translation.getLang() : translation.getName().getLang();
          translations.put(lang, translation.getName().getValue());
        }
      }

      name = TranslatedString.getDeduplicatedI18NString(translations, false);
    } else {
      name = new NonLocalizedString(stopPlace.getName().getValue());
    }
    return name;
  }

  /**
   * Map the centroid to a coordinate. If the centroid is missing or invalid, the mean coordinate
   * of the quays with valid coordinates is returned instead.
   *
   * @return {@code null} if no coordinate can be derived at all.
   */
  @Nullable
  private WgsCoordinate mapCoordinate(StopPlace stopPlace) {
    WgsCoordinate coordinate = mapCentroid(stopPlace);
    if (coordinate != null) {
      return coordinate;
    }
    List<WgsCoordinate> quayCoordinates = mapQuayCoordinates(stopPlace);
    if (quayCoordinates.isEmpty()) {
      issueStore.add(
        "StationWithoutValidCoordinates",
        "Station %s has no valid coordinates, neither on the StopPlace nor on any of its quays. " +
          "The StopPlace and its quays are skipped.",
        stopPlace.getId()
      );
      return null;
    }
    return WgsCoordinate.mean(quayCoordinates);
  }

  @Nullable
  private WgsCoordinate mapCentroid(StopPlace stopPlace) {
    try {
      WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(stopPlace.getCentroid());
      if (coordinate == null) {
        issueStore.add(
          "StationWithoutCoordinates",
          "Station %s %s does not contain any coordinates.",
          stopPlace.getId(),
          stopPlace.getName()
        );
      }
      return coordinate;
    } catch (InvalidCoordinateException e) {
      issueStore.add(
        "StationInvalidCoordinates",
        "Station %s has invalid coordinates. %s",
        stopPlace.getId(),
        e.getMessage()
      );
      return null;
    }
  }

  /**
   * Return the coordinates of the quays which have a valid coordinate. Quays with missing or
   * invalid coordinates are reported when the quays themselves are mapped.
   */
  private static List<WgsCoordinate> mapQuayCoordinates(StopPlace stopPlace) {
    if (stopPlace.getQuays() == null) {
      return List.of();
    }
    List<WgsCoordinate> coordinates = new ArrayList<>();
    var quays = JAXBUtils.streamJAXBElementValue(
      Quay.class,
      stopPlace.getQuays().getQuayRefOrQuay()
    ).toList();
    for (Quay quay : quays) {
      try {
        WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(quay.getCentroid());
        if (coordinate != null) {
          coordinates.add(coordinate);
        }
      } catch (InvalidCoordinateException ignore) {
        // Skip the quay, the issue is reported by the QuayMapper
      }
    }
    return coordinates;
  }
}
