package org.opentripplanner.updater;

import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.ext.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriVMUpdaterParameters;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdater;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdater;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdater;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdater;

import java.net.URI;
import java.util.List;

public interface UpdatersParameters {

  URI bikeRentalServiceDirectoryUrl();

  List<BikeRentalUpdater.Parameters> getBikeRentalParameters();

  List<GtfsRealtimeAlertsUpdater.Parameters> getGtfsRealtimeAlertsUpdaterParameters();

  List<PollingStoptimeUpdater.Parameters> getPollingStoptimeUpdaterParameters();

  List<SiriETUpdater.Parameters> getSiriETUpdaterParameters();

  List<SiriSXUpdaterParameters> getSiriSXUpdaterParameters();

  List<SiriVMUpdaterParameters> getSiriVMUpdaterParameters();

  List<WebsocketGtfsRealtimeUpdater.Parameters> getWebsocketGtfsRealtimeUpdaterParameters();

  List<MqttGtfsRealtimeUpdater.Parameters> getMqttGtfsRealtimeUpdaterParameters();

  List<PollingGraphUpdaterParameters> getBikeParkUpdaterParameters();

  List<PollingGraphUpdaterParameters> getExampleGraphUpdaterParameters();

  List<PollingGraphUpdaterParameters> getExamplePollingGraphUpdaterParameters();

  List<WFSNotePollingGraphUpdater.Parameters> getWinkkiPollingGraphUpdaterParameters();
}
