package org.opentripplanner.updater.trip.siri;

import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.updater.spi.UpdateResult;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;

public class SiriTestHelper {

  private final TransitTestEnvironment transitTestEnvironment;
  private final SiriRealTimeTripUpdateAdapter siriAdapter;
  private final SiriRealTimeTripUpdateAdapter siriAdapterWithFuzzyMatching;

  SiriTestHelper(TransitTestEnvironment transitTestEnvironment) {
    this.transitTestEnvironment = transitTestEnvironment;
    var repo = transitTestEnvironment.timetableRepository();
    this.siriAdapter = new SiriRealTimeTripUpdateAdapter(repo, DeduplicatorService.NOOP, null);
    var cache = new SiriFuzzyTripMatcherCache(repo);
    this.siriAdapterWithFuzzyMatching = new SiriRealTimeTripUpdateAdapter(
      repo,
      DeduplicatorService.NOOP,
      cache
    );
  }

  public static SiriTestHelper of(TransitTestEnvironment transitTestEnvironment) {
    return new SiriTestHelper(transitTestEnvironment);
  }

  public SiriEtBuilder etBuilder() {
    return new SiriEtBuilder(transitTestEnvironment.localTimeParser());
  }

  public UpdateResult applyEstimatedTimetableWithFuzzyMatcher(
    List<EstimatedTimetableDeliveryStructure> updates
  ) {
    return applyEstimatedTimetable(updates, true);
  }

  public UpdateResult applyEstimatedTimetable(List<EstimatedTimetableDeliveryStructure> updates) {
    return applyEstimatedTimetable(updates, false);
  }

  public TransitTestEnvironment realtimeTestEnvironment() {
    return transitTestEnvironment;
  }

  private UpdateResult applyEstimatedTimetable(
    List<EstimatedTimetableDeliveryStructure> updates,
    boolean fuzzyMatching
  ) {
    var resultRef = new AtomicReference<UpdateResult>();
    var adapter = fuzzyMatching ? siriAdapterWithFuzzyMatching : siriAdapter;
    try {
      transitTestEnvironment
        .updateManager()
        .submit(ctx -> {
          var buffer = ctx.repository(transitTestEnvironment.timetableHandle());
          var feedId = transitTestEnvironment.feedId();
          var transitService = new DefaultTransitService(
            transitTestEnvironment.timetableRepository(),
            buffer
          );
          resultRef.set(
            adapter
              .forUpdate(buffer)
              .applyEstimatedTimetable(
                new EntityResolver(transitService, feedId),
                feedId,
                DIFFERENTIAL,
                updates
              )
          );
        })
        .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return resultRef.get();
  }
}
