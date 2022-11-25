package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class ConflictingBikeTags implements DataImportIssue {

  public static final String FMT =
    "Conflicting tags bicycle:[yes|designated] and cycleway: " +
    "dismount on way %s, assuming dismount";
  public static final String HTMLFMT =
    "Conflicting tags bicycle:[yes|designated] and cycleway: " +
    "dismount on way <a href=\"http://www.openstreetmap.org/way/%d\">\"%d\"</a>, assuming dismount";

  final long wayId;

  public ConflictingBikeTags(long wayId) {
    this.wayId = wayId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, wayId);
  }

  @Override
  public String getHTMLMessage() {
    if (wayId > 0) {
      return String.format(HTMLFMT, wayId, wayId);
      // If way is lower then 0 it means it is temporary ID and so useless to link to OSM
    } else {
      return getMessage();
    }
  }
}
