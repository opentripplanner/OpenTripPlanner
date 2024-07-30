package org.opentripplanner.raptor.api.request;

import java.util.Collection;
import java.util.List;

public final class ViaLocation {

  private final String label;
  private final Collection<ViaConnection> connections;

  public ViaLocation(String label, Collection<ViaConnection> connections) {
    this.label = label;
    this.connections = validateConnections(connections);
  }

  public String label() {
    return label;
  }

  public Collection<ViaConnection> connections() {
    return connections;
  }

  @Override
  public String toString() {
    return "ViaLocation{label: " + label + ", connections: " + connections + "}";
  }

  private Collection<ViaConnection> validateConnections(Collection<ViaConnection> connections) {
    var list = List.copyOf(connections);

    // Compare all pairs to check for duplicates and none optimal connections
    for (int i = 0; i < list.size(); ++i) {
      var a = list.get(i);
      for (int j = i + 1; j < list.size(); ++j) {
        var b = list.get(j);
        if (a.equals(b) || a.isBetterThan(b) || b.isBetterThan(a)) {
          throw new IllegalArgumentException(
            "All connection need to be pareto-optimal. " + "a: " + a + ", b: " + b
          );
        }
      }
    }
    return list;
  }
}
