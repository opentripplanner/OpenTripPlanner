package org.opentripplanner.ext.flex;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transfer.regular.internal.DefaultTransferRepository;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.TestStopLocation;

class FlexTransferIndexTest {

  public static final TestStopLocation S1 = new TestStopLocation(id("1"));
  public static final Multimap<StopLocation, PathTransfer> TRANSFERS = ImmutableMultimap.of(
    S1,
    new PathTransfer(S1, S1, 100, List.of(), EnumSet.of(StreetMode.WALK))
  );

  @Test
  void indexOnce() {
    var index = new FlexTransferIndex();
    var repo = new DefaultTransferRepository(index);
    repo.addAllTransfersByStops(TRANSFERS);
    repo.index();
    assertThat(repo.findWalkTransfersToStop(S1)).isNotEmpty();
  }

  @Test
  void alreadyIndexed() {
    var index = new FlexTransferIndex();
    var repo = new DefaultTransferRepository(index);
    repo.addAllTransfersByStops(TRANSFERS);
    repo.index();

    assertThrows(IllegalStateException.class, repo::index);
  }
}
