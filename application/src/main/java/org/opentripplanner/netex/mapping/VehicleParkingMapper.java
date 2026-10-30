package org.opentripplanner.netex.mapping;

import static org.rutebanken.netex.model.ParkingVehicleEnumeration.CYCLE;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.E_CYCLE;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.PEDAL_CYCLE;

import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.WgsCoordinateMapper.InvalidCoordinateException;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Parking;
import org.rutebanken.netex.model.ParkingVehicleEnumeration;

/**
 * Maps from NeTEx Parking to an internal {@link VehicleParking}.
 */
class VehicleParkingMapper {

  private final FeedScopedIdFactory idFactory;

  private static final Set<ParkingVehicleEnumeration> BICYCLE_TYPES = Set.of(
    PEDAL_CYCLE,
    E_CYCLE,
    CYCLE
  );
  private final DataImportIssueStore issueStore;

  VehicleParkingMapper(FeedScopedIdFactory idFactory, DataImportIssueStore issueStore) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
  }

  @Nullable
  VehicleParking map(Parking parking) {
    if (parking.getTotalCapacity() == null) {
      issueStore.add(
        "MissingParkingCapacity",
        "NeTEx Parking '%s' does not contain totalCapacity",
        parkingDebugId(parking)
      );
      return null;
    }
    WgsCoordinate coordinate = mapCoordinate(parking);
    if (coordinate == null) {
      return null;
    }
    return VehicleParking.of()
      .id(idFactory.createId(parking.getId()))
      .name(mapName(parking))
      .coordinate(coordinate)
      .capacity(mapCapacity(parking))
      .bicyclePlaces(hasBikes(parking))
      .carPlaces(!hasBikes(parking))
      .entrance(mapEntrance(parking, coordinate))
      .build();
  }

  /**
   * The name is required by the APIs, so a missing name is reported and replaced by "N/A".
   */
  private I18NString mapName(Parking parking) {
    MultilingualString name = parking.getName();
    if (name != null && name.getValue() != null) {
      return new NonLocalizedString(name.getValue());
    }
    issueStore.add(
      "MissingParkingName",
      "NeTEx Parking '%s' does not contain a name",
      parkingDebugId(parking)
    );
    return new NonLocalizedString("N/A");
  }

  /**
   * Map the centroid to a coordinate. A parking lot without a valid coordinate cannot be linked
   * to the street network, so a missing or invalid centroid is reported and {@code null} is
   * returned.
   */
  @Nullable
  private WgsCoordinate mapCoordinate(Parking parking) {
    try {
      WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(parking.getCentroid());
      if (coordinate == null) {
        issueStore.add(
          "MissingParkingCoordinates",
          "NeTEx Parking '%s' does not contain a centroid",
          parkingDebugId(parking)
        );
      }
      return coordinate;
    } catch (InvalidCoordinateException e) {
      issueStore.add(
        "InvalidParkingCoordinates",
        "NeTEx Parking '%s' has invalid coordinates. %s",
        parkingDebugId(parking),
        e.getMessage()
      );
      return null;
    }
  }

  /**
   * In the Nordic profile many fields of {@link Parking} are optional so even adding the ID to the
   * issue store can lead to NPEs. For this reason we have a lot of fallbacks.
   */
  private static String parkingDebugId(Parking parking) {
    if (parking.getId() != null) {
      return parking.getId();
    } else if (parking.getName() != null) {
      return parking.getName().getValue();
    } else if (parking.getCentroid() != null) {
      return parking.getCentroid().toString();
    } else {
      return parking.toString();
    }
  }

  private VehicleParking.VehicleParkingEntranceCreator mapEntrance(
    Parking parking,
    WgsCoordinate coordinate
  ) {
    return builder ->
      builder
        .entranceId(idFactory.createId(parking.getId() + "/entrance"))
        .coordinate(coordinate)
        .walkAccessible(true)
        .carAccessible(true);
  }

  private static VehicleParkingSpaces mapCapacity(Parking parking) {
    var builder = VehicleParkingSpaces.of();
    int capacity = parking.getTotalCapacity().intValue();

    // we assume that if we have something bicycle-like in the vehicle types it's a bicycle parking
    // lot
    // it's not possible in NeTEx to split the spaces between the types, so if you want that
    // you have to define two parking lots with the same coordinates
    if (hasBikes(parking)) {
      builder.bicycleSpaces(capacity);
    } else {
      builder.carSpaces(capacity);
    }

    return builder.build();
  }

  private static boolean hasBikes(Parking parking) {
    return parking.getParkingVehicleTypes().stream().anyMatch(BICYCLE_TYPES::contains);
  }
}
