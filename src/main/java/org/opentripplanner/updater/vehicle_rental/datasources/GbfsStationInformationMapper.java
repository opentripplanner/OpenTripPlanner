package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.station_information.GBFSRentalUris;
import org.entur.gbfs.v2_2.station_information.GBFSStation;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystem;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Map;
import java.util.stream.Collectors;

public class GbfsStationInformationMapper {
    private final VehicleRentalSystem system;
    private final Map<String, RentalVehicleType> vehicleTypes;
    private final boolean allowKeepingRentedVehicleAtDestination;

    public GbfsStationInformationMapper(
            VehicleRentalSystem system,
            Map<String, RentalVehicleType> vehicleTypes,
            boolean allowKeepingRentedVehicleAtDestination
    ) {
        this.system = system;
        this.vehicleTypes = vehicleTypes;
        this.allowKeepingRentedVehicleAtDestination = allowKeepingRentedVehicleAtDestination;
    }

    public VehicleRentalStation mapStationInformation(GBFSStation station) {
        VehicleRentalStation rentalStation = new VehicleRentalStation();
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
        rentalStation.isVirtualStation = station.getIsVirtualStation() != null ? station.getIsVirtualStation() : false;
        rentalStation.isValetStation = station.getIsValetStation() != null ? station.getIsValetStation() : false;
        // TODO: Convert geometry
        // rentalStation.stationArea = station.getStationArea();
        rentalStation.capacity = station.getCapacity() != null ? station.getCapacity().intValue() : null;

        rentalStation.vehicleTypeAreaCapacity = station.getVehicleCapacity() != null && vehicleTypes != null
                ? station.getVehicleCapacity().getAdditionalProperties().entrySet().stream()
                    .collect(Collectors.toMap(e -> vehicleTypes.get(e.getKey()), e -> e.getValue().intValue()))
                : null;
        rentalStation.vehicleTypeDockCapacity = station.getVehicleTypeCapacity() != null && vehicleTypes != null
                ? station.getVehicleTypeCapacity().getAdditionalProperties().entrySet().stream()
                    .collect(Collectors.toMap(e -> vehicleTypes.get(e.getKey()), e -> e.getValue().intValue()))
                : null;

        rentalStation.isKeepingVehicleRentalAtDestinationAllowed = allowKeepingRentedVehicleAtDestination;

        GBFSRentalUris rentalUris = station.getRentalUris();
        if (rentalUris != null) {
            String androidUri = rentalUris.getAndroid();
            String iosUri = rentalUris.getIos();
            String webUri = rentalUris.getWeb();
            rentalStation.rentalUris = new VehicleRentalStationUris(androidUri, iosUri, webUri);
        }

        return rentalStation;
    }
}
