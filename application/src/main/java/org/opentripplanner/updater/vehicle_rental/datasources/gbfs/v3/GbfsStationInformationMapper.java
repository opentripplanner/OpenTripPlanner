package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedMapper.localizedString;
import static org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedMapper.optionalLocalizedString;

import java.util.Map;
import java.util.stream.Collectors;
import org.mobilitydata.gbfs.v3_0.station_information.*;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStationUris;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GbfsStationInformationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsStationInformationMapper.class);

  private final VehicleRentalSystem system;
  private final Map<String, RentalVehicleType> vehicleTypes;
  private final boolean allowKeepingRentedVehicleAtDestination;
  private final boolean overloadingAllowed;

  public GbfsStationInformationMapper(
    VehicleRentalSystem system,
    Map<String, RentalVehicleType> vehicleTypes,
    boolean allowKeepingRentedVehicleAtDestination,
    boolean overloadingAllowed
  ) {
    this.system = system;
    this.vehicleTypes = vehicleTypes;
    this.allowKeepingRentedVehicleAtDestination = allowKeepingRentedVehicleAtDestination;
    this.overloadingAllowed = overloadingAllowed;
  }

  public VehicleRentalStation mapStationInformation(GBFSStation station) {
    if (
      station.getStationId() == null ||
      station.getStationId().isBlank() ||
      station.getName() == null ||
      station.getName().isEmpty() ||
      station.getLon() == null ||
      station.getLat() == null
    ) {
      LOG.info(
        String.format(
          "GBFS station for %s system has issues with required fields: \n%s",
          system.systemId(),
          station
        )
      );
      return null;
    }
    var builder = VehicleRentalStation.of()
      .withId(new FeedScopedId(system.systemId(), station.getStationId()))
      .withSystem(system)
      .withLongitude(station.getLon())
      .withLatitude(station.getLat())
      .withName(localizedString(station.getName(), GBFSName::getLanguage, GBFSName::getText))
      .withShortName(
        optionalLocalizedString(
          station.getShortName(),
          GBFSShortName::getLanguage,
          GBFSShortName::getText
        )
      )
      .withCapacity(station.getCapacity() != null ? station.getCapacity().intValue() : null)
      .withIsArrivingInRentalVehicleAtDestinationAllowed(allowKeepingRentedVehicleAtDestination)
      .withOverloadingAllowed(overloadingAllowed);

    if (station.getVehicleTypesCapacity() != null && vehicleTypes != null) {
      builder.withVehicleTypeAreaCapacity(
        station
          .getVehicleTypesCapacity()
          .stream()
          .flatMap(e -> e.getVehicleTypeIds().stream().map(t -> Map.entry(t, e.getCount())))
          .collect(Collectors.toMap(e -> vehicleTypes.get(e.getKey()), Map.Entry::getValue))
      );
    }

    if (station.getVehicleDocksCapacity() != null && vehicleTypes != null) {
      builder.withVehicleTypeDockCapacity(
        station
          .getVehicleDocksCapacity()
          .stream()
          .flatMap(e -> e.getVehicleTypeIds().stream().map(t -> Map.entry(t, e.getCount())))
          .collect(Collectors.toMap(e -> vehicleTypes.get(e.getKey()), Map.Entry::getValue))
      );
    }

    GBFSRentalUris rentalUris = station.getRentalUris();
    if (rentalUris != null) {
      String androidUri = rentalUris.getAndroid();
      String iosUri = rentalUris.getIos();
      String webUri = rentalUris.getWeb();
      builder.withRentalUris(
        VehicleRentalStationUris.of()
          .withAndroid(androidUri)
          .withIos(iosUri)
          .withWeb(webUri)
          .build()
      );
    }

    return builder.build();
  }
}
