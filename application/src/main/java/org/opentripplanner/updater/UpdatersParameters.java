package org.opentripplanner.updater;

import java.util.List;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureETUpdaterParameters;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureSXUpdaterParameters;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.updater.alert.GtfsRealtimeAlertsUpdaterParameters;
import org.opentripplanner.updater.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.updater.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.updater.siri.updater.google.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.updater.siri.updater.lite.SiriETLiteUpdaterParameters;
import org.opentripplanner.updater.siri.updater.lite.SiriSXLiteUpdaterParameters;
import org.opentripplanner.updater.trip.MqttGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.trip.PollingTripUpdaterParameters;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;
import org.opentripplanner.updater.vehicle_position.VehiclePositionsUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;

public interface UpdatersParameters {
  TimetableSnapshotParameters timetableSnapshotParameters();

  VehicleRentalServiceDirectoryFetcherParameters getVehicleRentalServiceDirectoryFetcherParameters();

  List<VehicleRentalUpdaterParameters> getVehicleRentalParameters();

  List<GtfsRealtimeAlertsUpdaterParameters> getGtfsRealtimeAlertsUpdaterParameters();

  List<PollingTripUpdaterParameters> getPollingStoptimeUpdaterParameters();

  List<VehiclePositionsUpdaterParameters> getVehiclePositionsUpdaterParameters();

  List<SiriETUpdaterParameters> getSiriETUpdaterParameters();

  List<SiriETGooglePubsubUpdaterParameters> getSiriETGooglePubsubUpdaterParameters();

  List<SiriSXUpdaterParameters> getSiriSXUpdaterParameters();

  List<SiriETLiteUpdaterParameters> getSiriETLiteUpdaterParameters();

  List<SiriSXLiteUpdaterParameters> getSiriSXLiteUpdaterParameters();

  List<MqttGtfsRealtimeUpdaterParameters> getMqttGtfsRealtimeUpdaterParameters();

  List<VehicleParkingUpdaterParameters> getVehicleParkingUpdaterParameters();

  List<SiriAzureETUpdaterParameters> getSiriAzureETUpdaterParameters();

  List<SiriAzureSXUpdaterParameters> getSiriAzureSXUpdaterParameters();
}
