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
            new TurnRestrictionBad(restrictionTag.relationOSMID, "No from edge found")
          );
          continue;
        }
        if (restrictionTag.possibleTo.isEmpty()) {
          issueStore.add(new TurnRestrictionBad(restrictionTag.relationOSMID, "No to edge found"));
          continue;
        }
        for (StreetEdge from : restrictionTag.possibleFrom) {
          if (from == null) {
            issueStore.add(
              new TurnRestrictionBad(restrictionTag.relationOSMID, "from-edge is null")
            );
            continue;
          }
          for (StreetEdge to : restrictionTag.possibleTo) {
            if (to == null) {
              issueStore.add(
                new TurnRestrictionBad(restrictionTag.relationOSMID, "to-edge is null")
              );
              continue;
            }
            int angleDiff = from.getOutAngle() - to.getInAngle();
            if (angleDiff < 0) {
              angleDiff += 360;
            }
            switch (restrictionTag.direction) {
              case LEFT -> {
                if (angleDiff >= 160) {
                  issueStore.add(
                    new TurnRestrictionBad(
                      restrictionTag.relationOSMID,
                      "Left turn restriction is not on edges which turn left"
                    )
                  );
                  continue; // not a left turn
                }
              }
              case RIGHT -> {
                if (angleDiff <= 200) {
                  issueStore.add(
                    new TurnRestrictionBad(
                      restrictionTag.relationOSMID,
                      "Right turn restriction is not on edges which turn right"
                    )
                  );
                  continue; // not a right turn
                }
              }
              case U -> {
                if ((angleDiff <= 150 || angleDiff > 210)) {
                  issueStore.add(
                    new TurnRestrictionBad(
                      restrictionTag.relationOSMID,
                      "U-turn restriction is not on U-turn"
                    )
                  );
                  continue; // not a U turn
                }
              }
              case STRAIGHT -> {
                if (angleDiff >= 30 && angleDiff < 330) {
                  issueStore.add(
                    new TurnRestrictionBad(
                      restrictionTag.relationOSMID,
                      "Straight turn restriction is not on edges which go straight"
                    )
                  );
                  continue; // not straight
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
