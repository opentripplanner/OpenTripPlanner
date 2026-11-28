package org.opentripplanner.netex.mapping;

import org.entur.netex2.model.VehicleMeetingPoint;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.mapping.WgsCoordinateMapper.InvalidCoordinateException;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

/**
 * Maps a NeTEx VehicleMeetingPoint to an OTP stop. A meeting point assigned to a quay uses the stop
 * of the quay. Any other meeting point becomes a stop of its own, at the location of the meeting
 * point.
 */
class VehicleMeetingPointMapper {

  private final FeedScopedIdFactory idFactory;
  private final SiteRepositoryBuilder siteRepositoryBuilder;
  private final ReadOnlyHierarchicalMapById<VehicleMeetingPoint> vehicleMeetingPointById;
  private final ReadOnlyHierarchicalMap<String, String> quayIdByVehicleMeetingPointRef;

  VehicleMeetingPointMapper(
    FeedScopedIdFactory idFactory,
    SiteRepositoryBuilder siteRepositoryBuilder,
    ReadOnlyHierarchicalMapById<VehicleMeetingPoint> vehicleMeetingPointById,
    ReadOnlyHierarchicalMap<String, String> quayIdByVehicleMeetingPointRef
  ) {
    this.idFactory = idFactory;
    this.siteRepositoryBuilder = siteRepositoryBuilder;
    this.vehicleMeetingPointById = vehicleMeetingPointById;
    this.quayIdByVehicleMeetingPointRef = quayIdByVehicleMeetingPointRef;
  }

  /**
   * @return {@code null} if there is no stop for the meeting point.
   */
  @Nullable
  RegularStop mapToStop(@Nullable String vehicleMeetingPointId) {
    String quayId = quayIdByVehicleMeetingPointRef.lookup(vehicleMeetingPointId);
    if (quayId != null) {
      return siteRepositoryBuilder.regularStopsById().get(idFactory.createId(quayId));
    }

    VehicleMeetingPoint meetingPoint = vehicleMeetingPointById.lookup(vehicleMeetingPointId);
    if (meetingPoint == null) {
      return null;
    }
    return siteRepositoryBuilder.computeRegularStopIfAbsent(
      idFactory.createId(vehicleMeetingPointId),
      id -> map(id, meetingPoint)
    );
  }

  @Nullable
  private RegularStop map(FeedScopedId id, VehicleMeetingPoint meetingPoint) {
    WgsCoordinate coordinate = mapCoordinate(meetingPoint);
    if (coordinate == null) {
      return null;
    }
    String name = MultilingualStringMapper.nullableValueOf(meetingPoint.getName());

    return siteRepositoryBuilder
      .regularStop(id)
      .withName(new NonLocalizedString(name == null ? "N/A" : name))
      .withCoordinate(coordinate)
      .build();
  }

  @Nullable
  private static WgsCoordinate mapCoordinate(VehicleMeetingPoint meetingPoint) {
    try {
      return WgsCoordinateMapper.mapLocationToDomain(meetingPoint.getLocation());
    } catch (InvalidCoordinateException e) {
      return null;
    }
  }
}
