package org.opentripplanner.updater;

import org.opentripplanner.ext.siri.updater.SiriEstimatedTimetableGooglePubsubUpdater;
import org.opentripplanner.standalone.config.updaters.BikeRentalUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.GtfsRealtimeAlertsUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.MqttGtfsRealtimeUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.PollingGraphUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.PollingStoptimeUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriETUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriSXUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriVMUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.WFSNotePollingGraphUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.WebsocketGtfsRealtimeUpdaterParameters;

import java.net.URI;
import java.util.List;

public interface UpdaterParameters {

  URI bikeRentalServiceDirectoryUrl();

  List<BikeRentalUpdaterParameters> getBikeRentalParameters();

  List<GtfsRealtimeAlertsUpdaterParameters> getGtfsRealtimeAlertsUpdaterParameters();

  List<PollingStoptimeUpdaterParameters> getPollingStoptimeUpdaterParameters();

  List<SiriETUpdaterParameters> getSiriETUpdaterParameters();

  List<SiriEstimatedTimetableGooglePubsubUpdater.Parameters> getSiriETGooglePubsubUpdaterParameters();

  List<SiriSXUpdaterParameters> getSiriSXUpdaterParameters();

  List<SiriVMUpdaterParameters> getSiriVMUpdaterParameters();

  List<WebsocketGtfsRealtimeUpdaterParameters> getWebsocketGtfsRealtimeUpdaterParameters();

  List<MqttGtfsRealtimeUpdaterParameters> getMqttGtfsRealtimeUpdaterParameters();

  List<PollingGraphUpdaterParameters> getBikeParkUpdaterParameters();

  List<PollingGraphUpdaterParameters> getExampleGraphUpdaterParameters();

  List<PollingGraphUpdaterParameters> getExamplePollingGraphUpdaterParameters();

  List<WFSNotePollingGraphUpdaterParameters> getWinkkiPollingGraphUpdaterParameters();
}
