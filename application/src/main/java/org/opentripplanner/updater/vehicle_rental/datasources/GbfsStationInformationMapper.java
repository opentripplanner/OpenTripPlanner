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
    VehicleRentalStation rentalStation = new VehicleRentalStation();
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
          system.systemId,
          station
        )
      );
      return null;
    }
    rentalStation.id = new FeedScopedId(system.systemId, station.getStationId());
    rentalStation.system = system;
    rentalStation.longitude = station.getLon();
    rentalStation.latitude = station.getLat();
    rentalStation.name = new NonLocalizedString(station.getName());
    rentalStation.shortName = station.getShortName();
    rentalStation.address = station.getAddress();
    rentalStation.crossStreet = station.getCrossStreet();
    rentalStation.regionId = station.getRegionId();
    rentalStation.postCode = station.getPostCode();
    rentalStation.isVirtualStation = station.getIsVirtualStation() != null
      ? station.getIsVirtualStation()
      : false;
    rentalStation.isValetStation = station.getIsValetStation() != null
      ? station.getIsValetStation()
      : false;
    // TODO: Convert geometry
    // rentalStation.stationArea = station.getStationArea();
    rentalStation.capacity = station.getCapacity() != null
      ? station.getCapacity().intValue()
      : null;

    rentalStation.vehicleTypeAreaCapacity = station.getVehicleCapacity() != null &&
      vehicleTypes != null
      ? station
        .getVehicleCapacity()
        .getAdditionalProperties()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> vehicleTypes.get(e.getKey()), e -> e.getValue().intValue()))
      : null;
    rentalStation.vehicleTypeDockCapacity = station.getVehicleTypeCapacity() != null &&
      vehicleTypes != null
      ? station
        .getVehicleTypeCapacity()
        .getAdditionalProperties()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> vehicleTypes.get(e.getKey()), e -> e.getValue().intValue()))
      : null;

    rentalStation.isArrivingInRentalVehicleAtDestinationAllowed =
      allowKeepingRentedVehicleAtDestination;

    GBFSRentalUris rentalUris = station.getRentalUris();
    if (rentalUris != null) {
      String androidUri = rentalUris.getAndroid();
      String iosUri = rentalUris.getIos();
      String webUri = rentalUris.getWeb();
      rentalStation.rentalUris = new VehicleRentalStationUris(androidUri, iosUri, webUri);
    }
    rentalStation.overloadingAllowed = overloadingAllowed;
    return rentalStation;
  }
}
