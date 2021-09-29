package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.station_status.GBFSStation;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public class GbfsStationStatusMapper {

    private final Map<String, GBFSStation> statusLookup;
    private final Map<String, RentalVehicleType> vehicleTypes;

    public GbfsStationStatusMapper(Map<String, GBFSStation> statusLookup, Map<String, RentalVehicleType> vehicleTypes) {
        this.statusLookup = statusLookup;
        this.vehicleTypes = vehicleTypes;
    }

    void fillStationStatus(VehicleRentalStation station) {
        if (!statusLookup.containsKey(station.getStationId())) {
            station.realTimeData = false;
            return;
        }
        GBFSStation status = statusLookup.get(station.getStationId());

        station.vehiclesAvailable = status.getNumBikesAvailable() != null
                ? status.getNumBikesAvailable().intValue()
                : 0;

        station.vehicleTypesAvailable = status.getVehicleTypesAvailable() != null
                ? status.getVehicleTypesAvailable().stream()
                    .collect(Collectors.toMap(
                            e -> vehicleTypes.get(e.getVehicleTypeId()),
                            e -> e.getCount().intValue()
                    ))
                : Map.of(
                        RentalVehicleType.getDefaultType(station.getNetwork()),
                        station.vehiclesAvailable
                    );

        station.vehiclesDisabled = status.getNumBikesDisabled() != null
                ? status.getNumBikesDisabled().intValue()
                : 0;

        station.spacesAvailable = status.getNumDocksAvailable() != null
                ? status.getNumDocksAvailable().intValue()
                : Integer.MAX_VALUE;

        station.vehicleSpacesAvailable = status.getVehicleDocksAvailable() != null
                ? status.getVehicleDocksAvailable().stream()
                    .flatMap(available -> available.getVehicleTypeIds().stream().map(t ->
                            new T2<>(vehicleTypes.get(t), available.getCount().intValue())))
                    .collect(Collectors.toMap(t -> t.first, t -> t.second))
                : Map.of(
                        RentalVehicleType.getDefaultType(station.getNetwork()),
                        station.spacesAvailable
                    );

        station.spacesDisabled = status.getNumDocksDisabled() != null
                ? status.getNumDocksDisabled().intValue()
                : 0;

        station.isInstalled = status.getIsInstalled() != null ? status.getIsInstalled() : true;
        station.isRenting = status.getIsRenting() != null ? status.getIsRenting() : true;
        station.isReturning = status.getIsReturning() != null ? status.getIsReturning() : true;

        station.lastReported = status.getLastReported() != null
                ? Instant
                    .ofEpochSecond(status.getLastReported().longValue())
                    .atZone(station.system.timezone.toZoneId())
                : null;
    }
}
