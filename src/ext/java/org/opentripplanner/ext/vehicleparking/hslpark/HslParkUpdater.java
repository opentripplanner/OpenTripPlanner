package org.opentripplanner.ext.vehicleparking.hslpark;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.framework.io.JsonDataListDownloader;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingGroup;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces.VehicleParkingSpacesBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.DataSource;

/**
 * Vehicle parking updater class for https://github.com/HSLdevcom/parkandrideAPI format APIs. There
 * has been further development in a private repository (the current state is documented in
 * https://p.hsl.fi/docs/index.html) but this updater supports both formats.
 */
public class HslParkUpdater implements DataSource<VehicleParking> {

  private static final String JSON_PARSE_PATH = "results";

  private final HslFacilitiesDownloader facilitiesDownloader;
  private final int facilitiesFrequencySec;
  private final HslHubsDownloader hubsDownloader;
  private final JsonDataListDownloader utilizationsDownloader;
  private final HslParkToVehicleParkingMapper vehicleParkingMapper;
  private final HslHubToVehicleParkingGroupMapper vehicleParkingGroupMapper;
  private final HslParkUtilizationToPatchMapper parkPatchMapper;

  private long lastFacilitiesFetchTime;

  private List<VehicleParking> parks;
  private Map<FeedScopedId, VehicleParkingGroup> hubForPark;

  public HslParkUpdater(
    HslParkUpdaterParameters parameters,
    OpeningHoursCalendarService openingHoursCalendarService
  ) {
    String feedId = parameters.feedId();
    vehicleParkingMapper =
      new HslParkToVehicleParkingMapper(feedId, openingHoursCalendarService, parameters.timeZone());
    vehicleParkingGroupMapper = new HslHubToVehicleParkingGroupMapper(feedId);
    parkPatchMapper = new HslParkUtilizationToPatchMapper(feedId);
    facilitiesDownloader =
      new HslFacilitiesDownloader(
        parameters.facilitiesUrl(),
        JSON_PARSE_PATH,
        vehicleParkingMapper::parsePark
      );
    hubsDownloader =
      new HslHubsDownloader(
        parameters.hubsUrl(),
        JSON_PARSE_PATH,
        vehicleParkingGroupMapper::parseHub
      );
    utilizationsDownloader =
      new JsonDataListDownloader<>(
        parameters.utilizationsUrl(),
        "",
        parkPatchMapper::parseUtilization,
        Map.of()
      );
    this.facilitiesFrequencySec = parameters.facilitiesFrequencySec();
  }

  /**
   * Update the data from the sources. It first fetches parks from the facilities URL and park
   * groups from hubs URL and then real-time updates from utilizations URL. If facilitiesFrequencySec
   * is configured to be over 0, it also occasionally retches the parks as new parks might have been
   * added or the state of the old parks might have changed.
   *
   * @return true if there might have been changes
   */
  @Override
  public boolean update() {
    List<VehicleParking> parks = null;
    Map<FeedScopedId, VehicleParkingGroup> hubForPark;
    if (fetchFacilitiesAndHubsNow()) {
      hubForPark = hubsDownloader.downloadHubs();
      if (hubForPark != null) {
        parks = facilitiesDownloader.downloadFacilities(hubForPark);
        if (parks != null) {
          lastFacilitiesFetchTime = System.currentTimeMillis();
        }
      }
    } else {
      parks = this.parks;
      hubForPark = this.hubForPark;
    }
    if (parks != null) {
      List<HslParkPatch> utilizations = utilizationsDownloader.download();
      if (utilizations != null) {
        Map<FeedScopedId, List<HslParkPatch>> patches = utilizations
          .stream()
          .collect(Collectors.groupingBy(utilization -> utilization.getId()));
        parks.forEach(park -> {
          List<HslParkPatch> patchesForPark = patches.get(park.getId());
          if (patchesForPark != null) {
            park.updateAvailability(createVehicleAvailability(patchesForPark));
          }
        });
      } else if (this.parks != null) {
        return false;
      }
      synchronized (this) {
        // Update atomically
        this.parks = parks;
        this.hubForPark = hubForPark;
      }
      return true;
    }
    return false;
  }

  @Override
  public synchronized List<VehicleParking> getUpdates() {
    return parks;
  }

  private static VehicleParkingSpaces createVehicleAvailability(List<HslParkPatch> patches) {
    VehicleParkingSpacesBuilder availabilityBuilder = VehicleParkingSpaces.builder();
    boolean hasHandledSpaces = false;

    for (HslParkPatch patch : patches) {
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

  /**
   * @return true if facilities and hubs have not been successfully downloaded before, or
   * facilitiesFrequencySec > 0 and over facilitiesFrequencySec has passed since last successful
   * fetch
   */
  private boolean fetchFacilitiesAndHubsNow() {
    if (parks == null) {
      return true;
    }
    if (facilitiesFrequencySec <= 0) {
      return false;
    }
    return System.currentTimeMillis() > lastFacilitiesFetchTime + facilitiesFrequencySec * 1000;
  }
}
