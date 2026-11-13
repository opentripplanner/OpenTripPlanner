package org.opentripplanner.transit.transfer.regular.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

class DefaultRegularTransferRealtimeUpdaterTest {

  private final RecordingRepository repository = new RecordingRepository();
  private final DefaultRegularTransferRealtimeUpdater<String> subject =
    new DefaultRegularTransferRealtimeUpdater<>(repository);

  @Test
  void closeFacilitiesWritesToTheRepository() {
    subject.closeFacilities(Set.of("A", "B"));
    subject.closeFacilities(Set.of());

    assertEquals(List.of(Set.of("A", "B"), Set.of()), repository.closed);
  }

  @Test
  void updateTransfersForStopsIsNotImplemented() {
    var stop = new FeedScopedId("F", "1");

    assertThrows(UnsupportedOperationException.class, () -> subject.updateTransfersForStop(stop));
    assertThrows(UnsupportedOperationException.class, () ->
      subject.updateTransfersForStops(List.of(stop), path -> true)
    );
  }

  private static class RecordingRepository implements RegularTransferRepository<String> {

    private final List<Set<String>> closed = new ArrayList<>();

    @Override
    public void replaceTransfersFrom(
      AbstractUserPreferences<?> profile,
      int fromStop,
      Map<Integer, String> pathsByToStop
    ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void resetTransfersFrom(AbstractUserPreferences<?> profile, int fromStop) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void closeFacilities(Set<String> closedFacilityIds) {
      closed.add(closedFacilityIds);
    }
  }
}
