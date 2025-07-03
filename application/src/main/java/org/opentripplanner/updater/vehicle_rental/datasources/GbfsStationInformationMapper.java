package org.opentripplanner.updater.vehicle_rental.datasources;

import java.util.Map;
import java.util.stream.Collectors;
import org.mobilitydata.gbfs.v2_3.station_information.GBFSRentalUris;
import org.mobilitydata.gbfs.v2_3.station_information.GBFSStation;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStationUris;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbfsStationInformationMapper {

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
      station.getName().isBlank() ||
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
      .withName(new NonLocalizedString(station.getName()))
      .withShortName(station.getShortName())
      .withAddress(station.getAddress())
      .withCrossStreet(station.getCrossStreet())
      .withRegionId(station.getRegionId())
      .withPostCode(station.getPostCode())
      .withIsVirtualStation(
        station.getIsVirtualStation() != null ? station.getIsVirtualStation() : false
      )
      .withIsValetStation(station.getIsValetStation() != null ? station.getIsValetStation() : false)
      // TODO: Convert geometry
      // .withStationArea(station.getStationArea())
      .withCapacity(station.getCapacity() != null ? station.getCapacity().intValue() : null)
      .withIsArrivingInRentalVehicleAtDestinationAllowed(allowKeepingRentedVehicleAtDestination)
      .withOverloadingAllowed(overloadingAllowed);

    if (station.getVehicleCapacity() != null && vehicleTypes != null) {
      builder.withVehicleTypeAreaCapacity(
        station
          .getVehicleCapacity()
          .getAdditionalProperties()
          .entrySet()
          .stream()
          .collect(
            Collectors.toMap(e -> vehicleTypes.get(e.getKey()), e -> e.getValue().intValue())
          )
      );
    }

    if (station.getVehicleTypeCapacity() != null && vehicleTypes != null) {
      builder.withVehicleTypeDockCapacity(
        station
          .getVehicleTypeCapacity()
          .getAdditionalProperties()
          .entrySet()
          .stream()
          .collect(
            Collectors.toMap(e -> vehicleTypes.get(e.getKey()), e -> e.getValue().intValue())
          )
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
