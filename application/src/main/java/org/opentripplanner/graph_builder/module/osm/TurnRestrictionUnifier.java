package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.TurnRestrictionBad;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.edge.StreetEdge;

class TurnRestrictionUnifier {

  static void unifyTurnRestrictions(OsmDatabase osmdb, DataImportIssueStore issueStore) {
    // Note that usually when the from or to way is not found, it's because OTP has already
    // filtered that way. So many missing edges are not really problems worth issuing warnings on.
    for (Long fromWay : osmdb.getTurnRestrictionWayIds()) {
      for (TurnRestrictionTag restrictionTag : osmdb.getFromWayTurnRestrictions(fromWay)) {
        if (restrictionTag.possibleFrom.isEmpty()) {
          issueStore.add(
            new TurnRestrictionBad(restrictionTag.relationOsmID, "No from edge found")
          );
          continue;
        }
        if (restrictionTag.possibleTo.isEmpty()) {
          issueStore.add(new TurnRestrictionBad(restrictionTag.relationOsmID, "No to edge found"));
          continue;
        }
        for (StreetEdge from : restrictionTag.possibleFrom) {
          if (from == null) {
            issueStore.add(
              new TurnRestrictionBad(restrictionTag.relationOsmID, "from-edge is null")
            );
            continue;
          }
          for (StreetEdge to : restrictionTag.possibleTo) {
            if (to == null) {
              issueStore.add(
                new TurnRestrictionBad(restrictionTag.relationOsmID, "to-edge is null")
              );
              continue;
            }
            int angleDiff = from.getOutAngle() - to.getInAngle();
            if (angleDiff < 0) {
              angleDiff += 360;
            }
            // If the angle seems off for the stated restriction direction, add an issue
            // to the issue store, but do not ignore the restriction.
            switch (restrictionTag.direction) {
              case LEFT -> {
                if (angleDiff >= 160) {
                  issueStore.add(
                    new TurnRestrictionBad(
                      restrictionTag.relationOsmID,
                      "Left turn restriction is not on edges which turn left"
                    )
                  );
                }
              }
              case RIGHT -> {
                if (angleDiff <= 200) {
                  issueStore.add(
                    new TurnRestrictionBad(
                      restrictionTag.relationOsmID,
                      "Right turn restriction is not on edges which turn right"
                    )
                  );
                }
              }
              case U -> {
                if ((angleDiff <= 150 || angleDiff > 210)) {
                  issueStore.add(
                    new TurnRestrictionBad(
                      restrictionTag.relationOsmID,
                      "U-turn restriction is not on U-turn"
                    )
                  );
                }
              }
              case STRAIGHT -> {
                if (angleDiff >= 30 && angleDiff < 330) {
                  issueStore.add(
                    new TurnRestrictionBad(
                      restrictionTag.relationOsmID,
                      "Straight turn restriction is not on edges which go straight"
                    )
                  );
                }
              }
            }
            TurnRestriction restriction = new TurnRestriction(
              from,
              to,
              restrictionTag.type,
              restrictionTag.modes,
              restrictionTag.time
            );
            from.addTurnRestriction(restriction);
          }
        }
      }
    }
  }
}
