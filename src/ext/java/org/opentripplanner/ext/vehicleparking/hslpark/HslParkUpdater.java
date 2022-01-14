package org.opentripplanner.ext.vehicleparking.hslpark;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces.VehicleParkingSpacesBuilder;
import org.opentripplanner.updater.DataSource;
import org.opentripplanner.util.xml.JsonDataListDownloader;

/**
 * Vehicle parking updater class for https://github.com/HSLdevcom/parkandrideAPI format APIs. There
 * has been further development in a private repository (the current state is documented in
 * https://p.hsl.fi/docs/index.html) but this updater supports both formats.
 */
public class HslParkUpdater implements DataSource<VehicleParking> {

    private static final String JSON_PARSE_PATH = "results";

    private final JsonDataListDownloader facilitiesDownloader;
    private final int facilitiesFrequencySec;
    private final JsonDataListDownloader utilizationsDownloader;
    private final HslParkToVehicleParkingMapper vehicleParkingMapper;
    private final HslParkUtilizationToPatchMapper parkPatchMapper;

    private long lastFacilitiesFetchTime;

    private List<VehicleParking> parks;

    public HslParkUpdater(HslParkUpdaterParameters parameters) {
        String feedId = parameters.getFeedId();
        vehicleParkingMapper = new HslParkToVehicleParkingMapper(feedId);
        parkPatchMapper = new HslParkUtilizationToPatchMapper(feedId);
        facilitiesDownloader =
                new JsonDataListDownloader<>(
                        parameters.getFacilitiesUrl(),
                        JSON_PARSE_PATH,
                        vehicleParkingMapper::parsePark,
                        null
                );
        utilizationsDownloader =
                new JsonDataListDownloader<>(
                        parameters.getUtilizationsUrl(), "",
                        parkPatchMapper::parseUtilization,
                        null
                );
        this.facilitiesFrequencySec = parameters.getFacilitiesFrequencySec();
    }

    /**
     * Update the data from the sources. It first fetches parks from the facilities URL and then
     * realtime updates from utilizations URL. If facilitiesFrequencySec is configured to be over 0,
     * it also occasionally retches the parks as new parks might have been added or the state of the
     * old parks might have changed.
     *
     * @return true if there might have been changes
     */
    @Override
    public boolean update() {
        List<VehicleParking> parks;
        if (fetchFacilitiesNow()) {
            parks = facilitiesDownloader.download();
            if (parks != null) {
                lastFacilitiesFetchTime = System.currentTimeMillis();
            }
        }
        else {
            parks = this.parks;
        }
        if (parks != null) {
            List<HslParkPatch> utilizations = utilizationsDownloader.download();
            if (utilizations != null) {
                Map<FeedScopedId, List<HslParkPatch>> patches = utilizations.stream()
                        .collect(Collectors.groupingBy(utilization -> utilization.getId()));
                parks.forEach(park -> {
                    List<HslParkPatch> patchesForPark = patches.get(park.getId());
                    if (patchesForPark != null) {
                        park.updateAvailability(createVehicleAvailability(patchesForPark));
                    }
                });
            }
            else if (this.parks != null) {
                return false;
            }
            synchronized (this) {
                // Update atomically
                this.parks = parks;
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized List<VehicleParking> getUpdates() {
        return parks;
    }

    /**
     * @return true if facilities have not been successfully downloaded before, or
     * facilitiesFrequencySec > 0 and over facilitiesFrequencySec has passed since last successful
     * fetch
     */
    private boolean fetchFacilitiesNow() {
        if (parks == null) {
            return true;
        }
        if (facilitiesFrequencySec <= 0) {
            return false;
        }
        return System.currentTimeMillis() > lastFacilitiesFetchTime + facilitiesFrequencySec * 1000;
    }

    private static VehicleParkingSpaces createVehicleAvailability(List<HslParkPatch> patches) {
        VehicleParkingSpacesBuilder availabilityBuilder = VehicleParkingSpaces.builder();
        boolean hasHandledSpaces = false;

        for (int i = 0; i < patches.size(); i++) {
            HslParkPatch patch = patches.get(i);
            String type = patch.getCapacityType();

            if (type != null) {
                Integer spaces = patch.getSpacesAvailable();

                switch (type) {
                    case "CAR":
                        availabilityBuilder.carSpaces(spaces);
                        hasHandledSpaces = true;
                        break;
                    case "BICYCLE":
                        availabilityBuilder.bicycleSpaces(spaces);
                        hasHandledSpaces = true;
                        break;
                    case "DISABLED":
                        availabilityBuilder.wheelchairAccessibleCarSpaces(spaces);
                        hasHandledSpaces = true;
                        break;
                }
            }
        }

        return hasHandledSpaces ? availabilityBuilder.build() : null;
    }
}
