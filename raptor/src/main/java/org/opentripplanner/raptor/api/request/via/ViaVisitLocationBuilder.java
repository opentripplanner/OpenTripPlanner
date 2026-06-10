package org.opentripplanner.raptor.api.request.via;

import java.time.Duration;
import org.opentripplanner.raptor.spi.RaptorTransfer;

public final class ViaVisitLocationBuilder
  extends AbstractViaLocationBuilder<ViaVisitLocationBuilder> {

  private final int minimumWaitTime;

  public ViaVisitLocationBuilder(String label, Duration minimumWaitTime) {
    super(label);
    this.minimumWaitTime = RaptorViaLocation.validateMinimumWaitTime(minimumWaitTime);
  }

  public ViaVisitLocationBuilder addStop(int stop) {
    return addConnection(new RaptorVisitStopViaConnection(stop, minimumWaitTime));
  }

  public ViaVisitLocationBuilder addTransfer(int stop, RaptorTransfer transfer) {
    return addConnection(new RaptorTransferViaConnection(stop, minimumWaitTime, transfer));
  }

  public RaptorViaLocation build() {
    return new RaptorViaLocation(label, connections);
  }
}
