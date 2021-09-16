package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.free_bike_status.GBFSBike;
import org.entur.gbfs.v2_2.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_2.station_information.GBFSRentalUris;
import org.entur.gbfs.v2_2.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_2.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_2.system_information.GBFSSystemInformation;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
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

    /** Is it possible to arrive at the destination with a rented bicycle, without dropping it off */
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
    public List<VehicleRentalPlace> getStations() {

        // Get system information
        String system = loader.getFeed(GBFSSystemInformation.class).getData().getSystemId();

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
                VehicleRentalStation station = mapStationInformation(element, system);
                stations.add(station);
                if (!statusLookup.containsKey(element.getStationId())) {
                    station.realTimeData = false;
                    continue;
                }
                org.entur.gbfs.v2_2.station_status.GBFSStation status = statusLookup.get(element.getStationId());
                station.vehiclesAvailable = (int) (double) status.getNumBikesAvailable();
                if (status.getNumDocksAvailable() != null) {
                    station.spacesAvailable = (int) (double) status.getNumDocksAvailable();
                }
                station.allowDropoff = status.getIsReturning();
            }
        }

        // Append the floating bike stations.
        if (OTPFeature.FloatingBike.isOn()) {
            GBFSFreeBikeStatus freeBikeStatus = loader.getFeed(GBFSFreeBikeStatus.class);
            if (freeBikeStatus != null) {
                for (GBFSBike element : freeBikeStatus.getData().getBikes()) {
                    VehicleRentalPlace bike = mapFreeBike(element, system);
                    if (bike != null) {
                        stations.add(bike);
                    }
                }
            }
        }

        return stations;
    }

    public VehicleRentalStation mapStationInformation(org.entur.gbfs.v2_2.station_information.GBFSStation station, String system) {
        VehicleRentalStation rentalStation = new VehicleRentalStation();
        rentalStation.id = new FeedScopedId(system, station.getStationId());
        rentalStation.longitude = station.getLon();
        rentalStation.latitude = station.getLat();
        rentalStation.name = new NonLocalizedString(station.getName());
        rentalStation.isKeepingVehicleRentalAtDestinationAllowed = allowKeepingRentedVehicleAtDestination;
        rentalStation.isCarStation = routeAsCar;

        GBFSRentalUris rentalUris = station.getRentalUris();
        if (rentalUris != null) {
            String androidUri = rentalUris.getAndroid();
            String iosUri = rentalUris.getIos();
            String webUri = rentalUris.getWeb();
            rentalStation.rentalUris = new VehicleRentalStationUris(androidUri, iosUri, webUri);
        }

        return rentalStation;
    }

    public VehicleRentalVehicle mapFreeBike(GBFSBike bike, String system) {
        if ((bike.getStationId() == null || bike.getStationId().isBlank()) &&
            bike.getLon() != null &&
            bike.getLat() != null
        ) {
            VehicleRentalVehicle rentalVehicle = new VehicleRentalVehicle();
            rentalVehicle.id = new FeedScopedId(system, bike.getBikeId());
            rentalVehicle.name = new NonLocalizedString(bike.getBikeId());
            rentalVehicle.longitude = bike.getLon();
            rentalVehicle.latitude = bike.getLat();
            rentalVehicle.isCarStation = routeAsCar;
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
                .addBoolIfTrue("routeAsCar", routeAsCar)
                .addBoolIfTrue("allowKeepingRentedVehicleAtDestination", allowKeepingRentedVehicleAtDestination)
                .toString();
    }
}
