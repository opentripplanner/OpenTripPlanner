package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.rutebanken.netex.model.Quay;

class NetexMapperTest {

  private static final String QUAY_ID = "quay-1";
  private static final String SSP_ID = "ssp-1";
  private static final String FEED_ID = "sta";
  private static final RegularStop STOP = RegularStop.of(new FeedScopedId(FEED_ID, QUAY_ID), () -> 1
  ).build();
  private static final Deduplicator DEDUPLICATOR = new Deduplicator();

  @Test
  void sspWithAssignment() {
    var issueStore = new DefaultDataImportIssueStore();
    var transitBuilder = new OtpTransitServiceBuilder(SiteRepository.of().build(), issueStore);
    transitBuilder.siteRepository().withRegularStop(STOP);

    var netexMapper = new NetexMapper(
      transitBuilder,
      FEED_ID,
      DEDUPLICATOR,
      issueStore,
      Set.of(),
      Set.of(),
      10,
      false
    );

    var index = new NetexEntityIndex();
    index.quayById.add(new Quay().withId(QUAY_ID));
    index.quayIdByStopPointRef.add(SSP_ID, QUAY_ID);
    netexMapper.mapNetexToOtp(index.readOnlyView());

    assertEquals(
      STOP,
      transitBuilder.stopsByScheduledStopPoints().get(new FeedScopedId(FEED_ID, SSP_ID))
    );
  }

  @Test
  void sspPointsToUnknownId() {
    var issueStore = new DefaultDataImportIssueStore();

    var netexMapper = new NetexMapper(
      new OtpTransitServiceBuilder(SiteRepository.of().build(), issueStore),
      FEED_ID,
      DEDUPLICATOR,
      issueStore,
      Set.of(),
      Set.of(),
      10,
      false
    );

    var index = new NetexEntityIndex();
    index.quayById.add(new Quay().withId(QUAY_ID));
    index.quayIdByStopPointRef.add(SSP_ID, QUAY_ID);
    netexMapper.mapNetexToOtp(index.readOnlyView());

    var issueTypes = issueStore.listIssues().stream().map(DataImportIssue::getType).toList();

    assertThat(issueTypes).contains("ScheduledStopPointAssignedToUnknownQuay");
  }
}
