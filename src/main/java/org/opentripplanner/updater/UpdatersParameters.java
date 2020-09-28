package org.opentripplanner.updater;

import org.opentripplanner.standalone.config.updaters.BikeRentalUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.GtfsRealtimeAlertsUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.MqttGtfsRealtimeUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingStoptimeUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriETUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriSXUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriVMUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WFSNotePollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WebsocketGtfsRealtimeUpdaterConfig;

import java.net.URI;
import java.util.List;

public interface UpdatersParameters {

  URI bikeRentalServiceDirectoryUrl();

  List<BikeRentalUpdaterConfig> getBikeRentalParameters();

  List<GtfsRealtimeAlertsUpdaterConfig> getGtfsRealtimeAlertsUpdaterParameters();

  List<PollingStoptimeUpdaterConfig> getPollingStoptimeUpdaterParameters();

  List<SiriETUpdaterConfig> getSiriETUpdaterParameters();

  List<SiriSXUpdaterConfig> getSiriSXUpdaterParameters();

  List<SiriVMUpdaterConfig> getSiriVMUpdaterParameters();

  List<WebsocketGtfsRealtimeUpdaterConfig> getWebsocketGtfsRealtimeUpdaterParameters();

  List<MqttGtfsRealtimeUpdaterConfig> getMqttGtfsRealtimeUpdaterParameters();

  List<PollingGraphUpdaterConfig> getBikeParkUpdaterParameters();

  List<PollingGraphUpdaterConfig> getExampleGraphUpdaterParameters();

  List<PollingGraphUpdaterConfig> getExamplePollingGraphUpdaterParameters();

  List<WFSNotePollingGraphUpdaterConfig> getWinkkiPollingGraphUpdaterParameters();
}
