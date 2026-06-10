package org.opentripplanner.raptor.api.request.via;

import java.util.stream.IntStream;

public final class PassThroughLocationBuilder
  extends AbstractViaLocationBuilder<PassThroughLocationBuilder> {

  public PassThroughLocationBuilder(String label) {
    super(label);
  }

  public PassThroughLocationBuilder addStop(int stop) {
    return addConnection(new RaptorPassThroughViaConnection(stop));
  }

  public PassThroughLocationBuilder addStop(int... stops) {
    IntStream.of(stops).forEach(this::addStop);
    return this;
  }

  public RaptorViaLocation build() {
    return new RaptorViaLocation(label, connections);
  }
}
