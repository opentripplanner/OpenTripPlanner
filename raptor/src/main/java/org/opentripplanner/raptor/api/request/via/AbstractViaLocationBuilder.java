package org.opentripplanner.raptor.api.request.via;

import java.util.ArrayList;
import java.util.List;

abstract sealed class AbstractViaLocationBuilder<T extends AbstractViaLocationBuilder>
  permits ViaVisitLocationBuilder, PassThroughLocationBuilder {

  protected final String label;
  protected final List<ViaConnection> connections = new ArrayList<>();

  public AbstractViaLocationBuilder(String label) {
    this.label = label;
  }

  T addConnection(ViaConnection connection) {
    this.connections.add(connection);
    return (T) this;
  }
}
