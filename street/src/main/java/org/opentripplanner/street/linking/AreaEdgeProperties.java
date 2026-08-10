package org.opentripplanner.street.linking;

import java.util.List;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;

/**
 * The name, permission and safety factors a visibility edge borrows from the area(s) it crosses.
 * A multi-area group's sub-areas tile/overlap to cover the group, so a visibility edge routinely
 * crosses more than one. Rather than borrow a single (arbitrarily chosen) sub-area's properties,
 * the edge inherits the <em>worst case</em> over every sub-area it crosses: the permission is the
 * intersection (AND) of their permissions and the safety factors are the maximum (worst) of theirs,
 * so the edge is never more permissive or safer than any tile it physically crosses (e.g. an edge
 * crossing a {@code steps} tile cannot keep an adjacent square's flat walk cost or bike permission).
 * Wheelchair-accessibility is likewise merged with a logical AND, so any non-accessible tile the
 * edge crosses makes the whole edge non-accessible. The name is cosmetic and is taken from the first
 * crossed sub-area.
 */
record AreaEdgeProperties(
  I18NString name,
  StreetTraversalPermission permission,
  float bicycleSafety,
  float walkSafety,
  boolean wheelchairAccessible
) {
  /**
   * Folds the worst-case properties over every area in {@code crossed}, which must be non-empty (see
   * {@link PreparedAreaGroup#areasCrossedBy(org.locationtech.jts.geom.LineString)}).
   */
  static AreaEdgeProperties merge(List<Area> crossed) {
    var merged = of(crossed.getFirst());
    for (int i = 1; i < crossed.size(); i++) {
      merged = merged.mergeWith(crossed.get(i));
    }
    return merged;
  }

  private static AreaEdgeProperties of(Area area) {
    return new AreaEdgeProperties(
      area.getName(),
      area.getPermission(),
      area.getBicycleSafety(),
      area.getWalkSafety(),
      area.isWheelchairAccessible()
    );
  }

  private AreaEdgeProperties mergeWith(Area area) {
    return new AreaEdgeProperties(
      name,
      permission.intersection(area.getPermission()),
      Math.max(bicycleSafety, area.getBicycleSafety()),
      Math.max(walkSafety, area.getWalkSafety()),
      wheelchairAccessible && area.isWheelchairAccessible()
    );
  }
}
