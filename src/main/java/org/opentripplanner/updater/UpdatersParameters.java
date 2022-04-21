package org.opentripplanner.updater;

import java.util.List;
import org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriVMUpdaterParameters;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdaterParameters;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdaterParameters;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdaterParameters;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;
import org.opentripplanner.updater.vehicle_positions.VehiclePositionsUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;

public interface UpdatersParameters {
  VehicleRentalServiceDirectoryFetcherParameters getVehicleRentalServiceDirectoryFetcherParameters();

  List<VehicleRentalUpdaterParameters> getVehicleRentalParameters();

  List<GtfsRealtimeAlertsUpdaterParameters> getGtfsRealtimeAlertsUpdaterParameters();

  List<PollingStoptimeUpdaterParameters> getPollingStoptimeUpdaterParameters();

  List<VehiclePositionsUpdaterParameters> getVehiclePositionsUpdaterParameters();

  List<SiriETUpdaterParameters> getSiriETUpdaterParameters();

  List<SiriETGooglePubsubUpdaterParameters> getSiriETGooglePubsubUpdaterParameters();

  List<SiriSXUpdaterParameters> getSiriSXUpdaterParameters();

  List<SiriVMUpdaterParameters> getSiriVMUpdaterParameters();

  List<WebsocketGtfsRealtimeUpdaterParameters> getWebsocketGtfsRealtimeUpdaterParameters();

  List<MqttGtfsRealtimeUpdaterParameters> getMqttGtfsRealtimeUpdaterParameters();

  List<VehicleParkingUpdaterParameters> getVehicleParkingUpdaterParameters();

  List<WFSNotePollingGraphUpdaterParameters> getWinkkiPollingGraphUpdaterParameters();
}
