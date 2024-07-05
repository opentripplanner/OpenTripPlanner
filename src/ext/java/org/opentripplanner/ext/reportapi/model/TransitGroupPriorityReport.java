package org.opentripplanner.ext.reportapi.model;

import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.PriorityGroupConfigurator;
import org.opentripplanner.routing.api.request.request.TransitRequest;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * This class is used to report all transit-groups used for transit-group-priority. The report is
 * useful when configuring/debugging this functionality.
 * <p>
 * The format is pure text.
 */
public class TransitGroupPriorityReport {

  public static String build(Collection<TripPattern> patterns, TransitRequest request) {
    var c = PriorityGroupConfigurator.of(
      request.priorityGroupsByAgency(),
      request.priorityGroupsGlobal()
    );

    var map = new TreeMap<Integer, DebugEntity>();
    for (var it : patterns) {
      int groupId = c.lookupTransitGroupPriorityId(it);
      var de = map.computeIfAbsent(groupId, DebugEntity::new);
      de.add(
        it.getRoute().getAgency().getId().toString(),
        it.getMode().name(),
        it.getNetexSubmode().name()
      );
    }
    return (
      "TRANSIT GROUPS PRIORITY" +
      map.values().stream().map(DebugEntity::toString).sorted().collect(Collectors.joining(""))
    );
  }

  private static class DebugEntity {

    private final int groupId;
    private final TreeMap<String, AgencyEntry> agencies = new TreeMap<>();

    public DebugEntity(int groupId) {
      this.groupId = groupId;
    }

    void add(String agency, String mode, String submode) {
      agencies.computeIfAbsent(agency, AgencyEntry::new).add(mode, submode);
    }

    @Override
    public String toString() {
      var buf = new StringBuilder("\n  %#010x".formatted(groupId));
      for (var it : agencies.values()) {
        buf.append("\n    ").append(it.toString());
      }
      return buf.toString();
    }
  }

  private record AgencyEntry(String agency, TreeMap<String, TreeSet<String>> modes) {
    private AgencyEntry(String agency) {
      this(agency, new TreeMap<>());
    }

    void add(String mode, String submode) {
      modes.computeIfAbsent(mode, m -> new TreeSet<>()).add(submode);
    }

    @Override
    public String toString() {
      var buf = new StringBuilder();
      for (var it : modes.entrySet()) {
        buf.append(", ");
        buf.append(it.getKey());
        if (!it.getValue().isEmpty()) {
          buf.append(" (").append(String.join(", ", it.getValue())).append(")");
        }
      }
      return agency + " ~ " + buf.substring(2);
    }
  }
}
