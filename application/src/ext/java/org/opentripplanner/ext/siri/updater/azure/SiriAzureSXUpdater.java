package org.opentripplanner.ext.siri.updater.azure;

import java.time.Duration;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.alert.TransitAlertProvider;
import org.opentripplanner.updater.alert.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;

public class SiriAzureSXUpdater implements TransitAlertProvider, SiriAzureMessageHandler {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private final SiriAlertsUpdateHandler updateHandler;
  private final TransitAlertService transitAlertService;

  private WriteToGraphCallback saveResultOnGraph;

  public SiriAzureSXUpdater(
    SiriAzureSXUpdaterParameters config,
    TimetableRepository timetableRepository
  ) {
    this.transitAlertService = new TransitAlertServiceImpl(timetableRepository);
    this.updateHandler = new SiriAlertsUpdateHandler(
      config.feedId(),
      transitAlertService,
      Duration.ZERO
    );
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
  }

  @Override
  @Nullable
  public Future<?> handleMessage(ServiceDelivery serviceDelivery, String messageId) {
    var sxDeliveries = serviceDelivery.getSituationExchangeDeliveries();
    if (sxDeliveries == null || sxDeliveries.isEmpty()) {
      LOG.info("Empty Siri SX message {}", messageId);
      return null;
    } else {
      return saveResultOnGraph.execute(context -> updateHandler.update(serviceDelivery, context));
    }
  }

  @Override
  public TransitAlertService getTransitAlertService() {
    return this.transitAlertService;
  }
}
