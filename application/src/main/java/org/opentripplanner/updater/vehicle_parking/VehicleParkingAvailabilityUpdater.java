package org.opentripplanner.updater.vehicle_parking;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.RealTimeUpdateContext;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph updater that dynamically sets availability information on vehicle parking lots. This
 * updater fetches data from a single {@link DataSource<VehicleParking>}.
 */
public class VehicleParkingAvailabilityUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(
    VehicleParkingAvailabilityUpdater.class
  );
  private final DataSource<AvailabiltyUpdate> source;
  private final VehicleParkingRepository repository;

  public VehicleParkingAvailabilityUpdater(
    VehicleParkingUpdaterParameters parameters,
    DataSource<AvailabiltyUpdate> source,
    VehicleParkingRepository parkingRepository
  ) {
    super(parameters);
    this.source = source;
    this.repository = parkingRepository;

    LOG.info("Creating vehicle-parking updater running every {}: {}", pollingPeriod(), source);
  }

  @Override
  protected void runPolling() throws InterruptedException, ExecutionException {
    if (source.update()) {
      var updates = source.getUpdates();

      var graphWriterRunnable = new AvailabilityUpdater(updates);
      updateGraph(graphWriterRunnable);
    }
  }

  private class AvailabilityUpdater implements GraphWriterRunnable {

    private final List<AvailabiltyUpdate> updates;
    private final Map<FeedScopedId, VehicleParking> parkingById;

    private AvailabilityUpdater(List<AvailabiltyUpdate> updates) {
      this.updates = List.copyOf(updates);
      this.parkingById =
        repository
          .listVehicleParkings()
          .stream()
          .collect(Collectors.toUnmodifiableMap(VehicleParking::getId, Function.identity()));
    }

    @Override
    public void run(RealTimeUpdateContext context) {
      updates.forEach(this::handleUpdate);
    }

    private void handleUpdate(AvailabiltyUpdate update) {
      if (!parkingById.containsKey(update.vehicleParkingId())) {
        LOG.warn(
          "Parking with id {} does not exist. Skipping availability update.",
          update.vehicleParkingId()
        );
      } else {
        var parking = parkingById.get(update.vehicleParkingId());
        var builder = VehicleParkingSpaces.builder();
        if (parking.hasCarPlaces()) {
          builder.carSpaces(update.spacesAvailable());
        }
        if (parking.hasBicyclePlaces()) {
          builder.bicycleSpaces(update.spacesAvailable());
        }
        parking.updateAvailability(builder.build());
      }
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("source", source).toString();
  }
}
