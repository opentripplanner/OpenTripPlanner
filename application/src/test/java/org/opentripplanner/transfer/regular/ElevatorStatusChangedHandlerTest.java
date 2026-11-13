package org.opentripplanner.transfer.regular;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.updater.elevator.ElevatorStatusChanged;

class ElevatorStatusChangedHandlerTest {

  @Test
  void eventType() {
    assertEquals(ElevatorStatusChanged.class, new ElevatorStatusChangedHandler().eventType());
  }

  @Test
  void closesTheElevatorsOutOfServiceInTheTransfers() {
    var repository = new RecordingRepository();

    new ElevatorStatusChangedHandler().handle(
      new ElevatorStatusChanged(Set.of("Lift Maker:1")),
      repository
    );

    assertEquals(List.of(Set.of("Lift Maker:1")), repository.closed);
  }

  private static class RecordingRepository implements RegularTransferRepository<NearbyStop> {

    private final List<Set<String>> closed = new ArrayList<>();

    @Override
    public void replaceTransfersFrom(
      AbstractUserPreferences<?> profile,
      int fromStop,
      Map<Integer, NearbyStop> pathsByToStop
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
