package org.opentripplanner.ext.vehicleparking.liipi;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.framework.io.JsonDataListDownloader;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces.VehicleParkingSpacesBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vehicle parking updater class for Liipi format APIs. The format is documented in
 * https://parking.fintraffic.fi/docs/index.html).
 */
public class LiipiParkUpdater implements DataSource<VehicleParking> {

  private static final Logger LOG = LoggerFactory.getLogger(LiipiParkUpdater.class);

  private static final String JSON_PARSE_PATH = "results";

  private final LiipiFacilitiesDownloader facilitiesDownloader;
  private final int facilitiesFrequencySec;
  private final LiipiHubsDownloader hubsDownloader;
  private final JsonDataListDownloader utilizationsDownloader;
  private final LiipiParkToVehicleParkingMapper vehicleParkingMapper;
  private final LiipiHubToVehicleParkingGroupMapper vehicleParkingGroupMapper;
  private final LiipiParkUtilizationToPatchMapper parkPatchMapper;

  private long lastFacilitiesFetchTime;

  private List<VehicleParking> parks;
  private Map<FeedScopedId, VehicleParkingGroup> hubForPark;

  public LiipiParkUpdater(
    LiipiParkUpdaterParameters parameters,
    OpeningHoursCalendarService openingHoursCalendarService
  ) {
    String feedId = parameters.feedId();
    vehicleParkingMapper = new LiipiParkToVehicleParkingMapper(
      feedId,
      openingHoursCalendarService,
      parameters.timeZone()
    );
    vehicleParkingGroupMapper = new LiipiHubToVehicleParkingGroupMapper(feedId);
    parkPatchMapper = new LiipiParkUtilizationToPatchMapper(feedId);
    var otpHttpClientFactory = new OtpHttpClientFactory();
    facilitiesDownloader = new LiipiFacilitiesDownloader(
      parameters.facilitiesUrl(),
      JSON_PARSE_PATH,
      vehicleParkingMapper::parsePark,
      otpHttpClientFactory
    );
    hubsDownloader = new LiipiHubsDownloader(
      parameters.hubsUrl(),
      JSON_PARSE_PATH,
      vehicleParkingGroupMapper::parseHub,
      otpHttpClientFactory
    );
    utilizationsDownloader = new JsonDataListDownloader<>(
      parameters.utilizationsUrl(),
      "",
      parkPatchMapper::parseUtilization,
      Map.of(),
      otpHttpClientFactory.create(LOG)
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
      List<LiipiParkPatch> utilizations = utilizationsDownloader.download();
      if (utilizations != null) {
        Map<FeedScopedId, List<LiipiParkPatch>> patches = utilizations
          .stream()
          .collect(Collectors.groupingBy(utilization -> utilization.getId()));
        parks.forEach(park -> {
          List<LiipiParkPatch> patchesForPark = patches.get(park.getId());
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

  private static VehicleParkingSpaces createVehicleAvailability(List<LiipiParkPatch> patches) {
    VehicleParkingSpacesBuilder availabilityBuilder = VehicleParkingSpaces.builder();
    boolean hasHandledSpaces = false;

    for (LiipiParkPatch patch : patches) {
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
