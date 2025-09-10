package org.opentripplanner.ext.carpooling.updater;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.support.siri.SiriFileLoader;
import org.opentripplanner.updater.support.siri.SiriHttpLoader;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableSource;
import org.opentripplanner.updater.trip.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.ServiceDelivery;

/**
 * Update OTP stop timetables from some a Siri-ET HTTP sources.
 */
public class SiriETCarpoolingUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SiriETCarpoolingUpdater.class);
  /**
   * Update streamer
   */
  private final EstimatedTimetableSource updateSource;

  private final CarpoolingRepository repository;

  private final CarpoolSiriMapper mapper;

  /**
   * Feed id that is used for the trip ids in the TripUpdates
   */
  private final String feedId;

  private final Consumer<UpdateResult> metricsConsumer;

  public SiriETCarpoolingUpdater(
    SiriETCarpoolingUpdaterParameters config,
    CarpoolingRepository repository,
    Graph graph,
    VertexLinker vertexLinker,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    super(config);
    this.feedId = config.feedId();

    SiriLoader siriHttpLoader = siriLoader(config);
    updateSource = new SiriETHttpTripUpdateSource(config.sourceParameters(), siriHttpLoader);

    this.repository = repository;

    this.blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();

    LOG.info("Creating SIRI-ET updater running every {}: {}", pollingPeriod(), updateSource);

    this.metricsConsumer = TripUpdateMetrics.streaming(config);

    this.mapper = new CarpoolSiriMapper(graph, vertexLinker, streetLimitationParametersService);
  }

  /**
   * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
   * applies those updates to the graph.
   */
  @Override
  public void runPolling() {
    boolean moreData = false;
    do {
      var updates = updateSource.getUpdates();
      if (updates.isPresent()) {
        ServiceDelivery serviceDelivery = updates.get().getServiceDelivery();
        moreData = Boolean.TRUE.equals(serviceDelivery.isMoreData());
        List<EstimatedTimetableDeliveryStructure> etds =
          serviceDelivery.getEstimatedTimetableDeliveries();
        if (etds != null) {
          for (EstimatedTimetableDeliveryStructure etd : etds) {
            var ejvfs = etd.getEstimatedJourneyVersionFrames();
            for (var ejvf : ejvfs) {
              if (ejvf.getEstimatedVehicleJourneies() == null) {
                LOG.warn("Received an empty EstimatedJourneyVersionFrame, skipping");
                continue;
              }
              ejvf
                .getEstimatedVehicleJourneies()
                .forEach(ejv -> {
                  var carpoolTrip = mapper.mapSiriToCarpoolTrip(ejv);
                  if (carpoolTrip != null) {
                    repository.addCarpoolTrip(carpoolTrip);
                  }
                });
            }
          }
        }
      }
    } while (moreData);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SiriETCarpoolingUpdater.class)
      .addStr("source", updateSource.toString())
      .addDuration("frequency", pollingPeriod())
      .toString();
  }

  public interface Parameters extends UrlUpdaterParameters, PollingGraphUpdaterParameters {
    String url();

    boolean blockReadinessUntilInitialized();

    boolean fuzzyTripMatching();
  }

  private static SiriLoader siriLoader(SiriETCarpoolingUpdaterParameters config) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(config.url())) {
      return new SiriFileLoader(config.url());
    }
    return new SiriHttpLoader(
      config.url(),
      config.timeout(),
      config.httpRequestHeaders(),
      config.previewInterval()
    );
  }
}
