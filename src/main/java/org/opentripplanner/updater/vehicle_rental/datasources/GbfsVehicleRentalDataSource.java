package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.free_bike_status.GBFSBike;
import org.entur.gbfs.v2_2.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_2.station_information.GBFSRentalUris;
import org.entur.gbfs.v2_2.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_2.station_status.GBFSStation;
import org.entur.gbfs.v2_2.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_2.system_information.GBFSSystemInformation;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    /** Some car rental systems and flex transit systems work exactly like bike rental, but with cars. */
    private final boolean routeAsCar;

    private final boolean allowKeepingRentedVehicleAtDestination;

    private GbfsFeedLoader loader;

    public GbfsVehicleRentalDataSource(GbfsVehicleRentalDataSourceParameters parameters) {
        url = parameters.getUrl();
        language = parameters.language();
        httpHeaders = parameters.getHttpHeaders();
        routeAsCar = parameters.routeAsCar();
        allowKeepingRentedVehicleAtDestination = parameters.allowKeepingRentedVehicleAtDestination();
    }

    @Override
    public void setup() {
        loader = new GbfsFeedLoader(url, httpHeaders, language);
    }

    @Override
    public boolean update() {
        if (loader == null) {return false;}
        return loader.update();
    }

    @Override
    public List<VehicleRentalStation> getStations() {

        // Get system information
        String system = loader.getFeed(GBFSSystemInformation.class).getData().getSystemId();

        List<VehicleRentalStation> stations = new LinkedList<>();

        // Index all the station status entries on their station ID.
        Map<FeedScopedId, VehicleRentalStation> statusLookup = new HashMap<>();

        // Station status is required for all systems using stations
        GBFSStationStatus stationStatus = loader.getFeed(GBFSStationStatus.class);
        if (stationStatus != null) {
            for (var element : stationStatus.getData().getStations()) {
                VehicleRentalStation station = mapStationStatus(element, system);
                statusLookup.put(station.id, station);
            }

            // Iterate over all known stations, and if we have any status information add it to those station objects.
            for (var element : loader.getFeed(GBFSStationInformation.class).getData().getStations()) {
                VehicleRentalStation station = mapStationInformation(element, system);
                stations.add(station);
                if (!statusLookup.containsKey(station.id)) { continue; }
                VehicleRentalStation status = statusLookup.get(station.id);
                station.vehiclesAvailable = status.vehiclesAvailable;
                station.spacesAvailable = status.spacesAvailable;
            }
        }

        // Append the floating bike stations.
        if (OTPFeature.FloatingBike.isOn()) {
            GBFSFreeBikeStatus freeBikeStatus = loader.getFeed(GBFSFreeBikeStatus.class);
            if (freeBikeStatus != null) {
                for (GBFSBike element : freeBikeStatus.getData().getBikes()) {
                    VehicleRentalStation bike = mapFreeBike(element, system);
                    if (bike != null) {
                        stations.add(bike);
                    }
                }
            }
        }

        return stations;
    }

    public VehicleRentalStation mapStationInformation(org.entur.gbfs.v2_2.station_information.GBFSStation station, String system) {
        VehicleRentalStation brstation = new VehicleRentalStation();
        brstation.id = new FeedScopedId(system, station.getStationId());
        brstation.longitude = station.getLon();
        brstation.latitude = station.getLat();
        brstation.name = new NonLocalizedString(station.getName());
        brstation.isKeepingVehicleRentalAtDestinationAllowed = allowKeepingRentedVehicleAtDestination;
        brstation.isCarStation = routeAsCar;

        GBFSRentalUris rentalUris = station.getRentalUris();
        if (rentalUris != null) {
            String androidUri = rentalUris.getAndroid();
            String iosUri = rentalUris.getIos();
            String webUri = rentalUris.getWeb();
            brstation.rentalUris = new VehicleRentalStationUris(androidUri, iosUri, webUri);
        }

        return brstation;
    }

    public VehicleRentalStation mapStationStatus(GBFSStation station, String system) {
        VehicleRentalStation brstation = new VehicleRentalStation();
        brstation.id = new FeedScopedId(system, station.getStationId());
        brstation.vehiclesAvailable = (int) (double) station.getNumBikesAvailable();
        if (station.getNumDocksAvailable() != null) {
            brstation.spacesAvailable = (int) (double) station.getNumDocksAvailable();
        }
        brstation.isKeepingVehicleRentalAtDestinationAllowed = allowKeepingRentedVehicleAtDestination;
        brstation.isCarStation = routeAsCar;
        return brstation;
    }

    public VehicleRentalStation mapFreeBike(GBFSBike bike, String system) {
        if ((bike.getStationId() == null || bike.getStationId().isBlank()) &&
            bike.getLon() != null &&
            bike.getLat() != null
        ) {
            VehicleRentalStation brstation = new VehicleRentalStation();
            brstation.id = new FeedScopedId(system, bike.getBikeId());
            brstation.name = new NonLocalizedString(bike.getBikeId());
            brstation.longitude = bike.getLon();
            brstation.latitude = bike.getLat();
            brstation.vehiclesAvailable = 1;
            brstation.spacesAvailable = 0;
            brstation.allowDropoff = false;
            brstation.isFloatingBike = true;
            brstation.isCarStation = routeAsCar;
            return brstation;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(GbfsVehicleRentalDataSource.class)
                .addStr("url", url)
                .addStr("language", language)
                .addBoolIfTrue("routeAsCar", routeAsCar)
                .addBoolIfTrue("allowKeepingRentedVehicleAtDestination", allowKeepingRentedVehicleAtDestination)
                .toString();
    }
}
