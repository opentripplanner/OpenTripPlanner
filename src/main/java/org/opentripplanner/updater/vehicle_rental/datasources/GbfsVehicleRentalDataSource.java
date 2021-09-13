package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.free_bike_status.GBFSBike;
import org.entur.gbfs.v2_2.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_2.station_information.GBFSRentalUris;
import org.entur.gbfs.v2_2.station_information.GBFSStation;
import org.entur.gbfs.v2_2.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_2.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_2.system_information.GBFSData;
import org.entur.gbfs.v2_2.system_information.GBFSSystemInformation;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleType;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleTypes;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystem;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by demory on 2017-03-14.
 *
 * Leaving OTPFeature.FloatingBike turned off both prevents floating bike updaters added to
 * router-config.json from being used, but more importantly, floating bikes added by a
 * VehicleRentalServiceDirectoryFetcher endpoint (which may be outside our control) will not be used.
 */
class GbfsVehicleRentalDataSource implements VehicleRentalDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GbfsVehicleRentalDataSource.class);

    private final String url;

    private final String language;

    private final Map<String, String> httpHeaders;

    /** Is it possible to arrive at the destination with a rented bicycle, without dropping it off */
    private final boolean allowKeepingRentedVehicleAtDestination;

    private GbfsFeedLoader loader;

    public GbfsVehicleRentalDataSource(GbfsVehicleRentalDataSourceParameters parameters) {
        url = parameters.getUrl();
        language = parameters.language();
        httpHeaders = parameters.getHttpHeaders();
        allowKeepingRentedVehicleAtDestination = parameters.allowKeepingRentedVehicleAtDestination();
    }

    @Override
    public void setup() {
        loader = new GbfsFeedLoader(url, httpHeaders, language);
    }

    @Override
    public boolean update() {
        if (loader == null) {
            return false;
        }
        return loader.update();
    }

    @Override
    public List<VehicleRentalPlace> getStations() {

        // Get system information
        VehicleRentalSystem system = mapSystemInformation(loader.getFeed(GBFSSystemInformation.class).getData());
        GBFSVehicleTypes rawVehicleTypes = loader.getFeed(GBFSVehicleTypes.class);
        Map<String, RentalVehicleType> vehicleTypes = null;
        if (rawVehicleTypes != null) {
            vehicleTypes = rawVehicleTypes.getData().getVehicleTypes().stream()
                    .map(v -> mapRentalVehicleType(v, system.systemId))
                    .collect(Collectors.toMap(v -> v.id.getId(), Function.identity()));
        }

        List<VehicleRentalPlace> stations = new LinkedList<>();

        // Index all the station status entries on their station ID.
        Map<String, org.entur.gbfs.v2_2.station_status.GBFSStation> statusLookup = new HashMap<>();

        // Station status is required for all systems using stations
        GBFSStationStatus stationStatus = loader.getFeed(GBFSStationStatus.class);
        if (stationStatus != null) {
            for (var element : stationStatus.getData().getStations()) {
                statusLookup.put(element.getStationId(), element);
            }

            // Iterate over all known stations, and if we have any status information add it to those station objects.
            for (var element : loader.getFeed(GBFSStationInformation.class).getData().getStations()) {
                VehicleRentalStation station = mapStationInformation(element, system, vehicleTypes);
                stations.add(station);
                mapStationStatus(statusLookup, station, vehicleTypes);
            }
        }

        // Append the floating bike stations.
        if (OTPFeature.FloatingBike.isOn()) {
            GBFSFreeBikeStatus freeBikeStatus = loader.getFeed(GBFSFreeBikeStatus.class);
            if (freeBikeStatus != null) {
                for (GBFSBike element : freeBikeStatus.getData().getBikes()) {
                    VehicleRentalPlace bike = mapFreeBike(element, system, vehicleTypes);
                    if (bike != null) {
                        stations.add(bike);
                    }
                }
            }
        }

        return stations;
    }

    private VehicleRentalSystem mapSystemInformation(GBFSData systemInformation) {
        VehicleRentalSystem.AppInformation android = null;
        VehicleRentalSystem.AppInformation ios = null;

        if (systemInformation.getRentalApps() != null) {
            if (systemInformation.getRentalApps().getAndroid() != null) {
                try {
                    android = new VehicleRentalSystem.AppInformation(
                            new URI(systemInformation.getRentalApps().getAndroid().getStoreUri()),
                            new URI(systemInformation.getRentalApps().getAndroid().getDiscoveryUri())
                    );
                } catch (URISyntaxException e) {
                    LOG.warn("Unable to parse rental URIs");
                }
            }
            if (systemInformation.getRentalApps().getIos() != null) {
                try {
                    ios = new VehicleRentalSystem.AppInformation(
                            new URI(systemInformation.getRentalApps().getIos().getStoreUri()),
                            new URI(systemInformation.getRentalApps().getIos().getDiscoveryUri())
                    );
                } catch (URISyntaxException e) {
                    LOG.warn("Unable to parse rental URIs");
                }
            }
        }

        URL url = null;
        if (systemInformation.getUrl() != null) {
            try {
                url = new URL(systemInformation.getUrl());
            } catch (MalformedURLException e) {
                LOG.warn("Unable to parse system URL");
            }
        }

        URL purchaseUrl = null;
        if (systemInformation.getPurchaseUrl() != null) {
            try {
                purchaseUrl = new URL(systemInformation.getPurchaseUrl());
            } catch (MalformedURLException e) {
                LOG.warn("Unable to parse system URL");
            }
        }

        return new VehicleRentalSystem(
                systemInformation.getSystemId(),
                Locale.forLanguageTag(systemInformation.getLanguage()),
                systemInformation.getName(),
                systemInformation.getShortName(),
                systemInformation.getOperator(),
                url,
                purchaseUrl,
                systemInformation.getStartDate() != null
                        ? LocalDate.parse(systemInformation.getStartDate())
                        : null,
                systemInformation.getPhoneNumber(),
                systemInformation.getEmail(),
                systemInformation.getFeedContactEmail(),
                TimeZone.getTimeZone(systemInformation.getTimezone()),
                systemInformation.getLicenseUrl(),
                android,
                ios
        );
    }

    private RentalVehicleType mapRentalVehicleType(GBFSVehicleType vehicleType, String systemId) {
        return new RentalVehicleType(
                new FeedScopedId(systemId, vehicleType.getVehicleTypeId()),
                vehicleType.getName(),
                RentalVehicleType.FormFactor.fromGbfs(vehicleType.getFormFactor()),
                RentalVehicleType.PropulsionType.fromGbfs(vehicleType.getPropulsionType()),
                vehicleType.getMaxRangeMeters()
        );
    }

    private VehicleRentalStation mapStationInformation(GBFSStation station, VehicleRentalSystem system, Map<String, RentalVehicleType> vehicleTypes) {
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
        rentalStation.capacity = station.getCapacity() != null ? (int) (double) station.getCapacity() : null;
        rentalStation.vehicleTypeAreaCapacity = station.getVehicleCapacity() != null && vehicleTypes != null
                ? station.getVehicleCapacity().getAdditionalProperties().entrySet().stream()
                    .collect(Collectors.toMap(e -> vehicleTypes.get(e.getKey()), e -> (int) (double) e.getValue()))
                : null;
        rentalStation.vehicleTypeDockCapacity = station.getVehicleTypeCapacity() != null && vehicleTypes != null
                ? station.getVehicleTypeCapacity().getAdditionalProperties().entrySet().stream()
                .collect(Collectors.toMap(e -> vehicleTypes.get(e.getKey()), e -> (int) (double) e.getValue()))
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

    private void mapStationStatus(
            Map<String, org.entur.gbfs.v2_2.station_status.GBFSStation> statusLookup,
            VehicleRentalStation station,
            Map<String, RentalVehicleType> vehicleTypes
    ) {
        if (!statusLookup.containsKey(station.getStationId())) {
            station.realTimeData = false;
            return;
        }
        org.entur.gbfs.v2_2.station_status.GBFSStation status = statusLookup.get(station.getStationId());

        station.vehiclesAvailable = status.getNumBikesAvailable() != null ? (int) (double) status.getNumBikesAvailable() : 0;
        station.vehicleTypesAvailable = status.getVehicleTypesAvailable() != null
                ? status.getVehicleTypesAvailable().stream()
                    .collect(Collectors.toMap(e -> vehicleTypes.get(e.getVehicleTypeId()), e -> (int) (double) e.getCount()))
                : null;
        station.vehiclesDisabled = status.getNumBikesDisabled() != null ? (int) (double) status.getNumBikesDisabled(): 0;

        station.spacesAvailable = status.getNumDocksAvailable() != null ? (int) (double) status.getNumDocksAvailable() : Integer.MAX_VALUE;
        station.vehicleSpacesAvailable = status.getVehicleDocksAvailable() != null
                ? status.getVehicleDocksAvailable().stream()
                    .flatMap(available -> available.getVehicleTypeIds().stream()
                            .map(t -> new T2<>(vehicleTypes.get(t), (int) (double) available.getCount())))
                    .collect(Collectors.toMap(t -> t.first, t -> t.second))
                : null;
        station.spacesDisabled = status.getNumDocksDisabled() != null ? (int) (double) status.getNumDocksDisabled() : 0;

        station.isInstalled = status.getIsInstalled() != null ? status.getIsInstalled() : true;
        station.isRenting = status.getIsRenting() != null ? status.getIsRenting() : true;
        station.isReturning = status.getIsReturning() != null ? status.getIsReturning() : true;

        station.lastReported = status.getLastReported() != null
                ? Instant.ofEpochSecond((long) (double) status.getLastReported()).atZone(station.system.timezone.toZoneId())
                : null;
    }

    private VehicleRentalVehicle mapFreeBike(GBFSBike vehicle, VehicleRentalSystem system, Map<String, RentalVehicleType> vehicleTypes) {
        if ((vehicle.getStationId() == null || vehicle.getStationId().isBlank()) &&
                vehicle.getLon() != null &&
                vehicle.getLat() != null
        ) {
            VehicleRentalVehicle rentalVehicle = new VehicleRentalVehicle();
            rentalVehicle.id = new FeedScopedId(system.systemId, vehicle.getBikeId());
            rentalVehicle.system = system;
            rentalVehicle.name = new NonLocalizedString(vehicle.getBikeId());
            rentalVehicle.longitude = vehicle.getLon();
            rentalVehicle.latitude = vehicle.getLat();
            rentalVehicle.vehicleType = vehicleTypes == null
                    ? RentalVehicleType.getDefaultType(system.systemId)
                    : vehicleTypes.get(vehicle.getVehicleTypeId());
            rentalVehicle.isReserved = vehicle.getIsReserved();
            rentalVehicle.isDisabled = vehicle.getIsDisabled();
            rentalVehicle.lastReported = vehicle.getLastReported() != null
                    ? Instant.ofEpochSecond((long) (double) vehicle.getLastReported()).atZone(system.timezone.toZoneId())
                    : null;
            rentalVehicle.currentRangeMeters = vehicle.getCurrentRangeMeters();
            rentalVehicle.pricingPlanId = vehicle.getPricingPlanId();
            org.entur.gbfs.v2_2.free_bike_status.GBFSRentalUris rentalUris = vehicle.getRentalUris();
            if (rentalUris != null) {
                String androidUri = rentalUris.getAndroid();
                String iosUri = rentalUris.getIos();
                String webUri = rentalUris.getWeb();
                rentalVehicle.rentalUris = new VehicleRentalStationUris(androidUri, iosUri, webUri);
            }

            return rentalVehicle;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(GbfsVehicleRentalDataSource.class)
                .addStr("url", url)
                .addStr("language", language)
                .addBoolIfTrue("allowKeepingRentedVehicleAtDestination", allowKeepingRentedVehicleAtDestination)
                .toString();
    }
}
