package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class ParkAndRideUnlinked implements DataImportIssue {

  public static final String FMT =
    "Park and ride '%s' (%s) not linked to any streets; it will not be usable.";
  public static final String HTMLFMT =
    "Park and ride <a href='%s'>'%s' (%s)</a> not linked to any streets; it will not be usable.";

  final String name;
  final OSMWithTags entity;

  public ParkAndRideUnlinked(String name, OSMWithTags entity) {
    this.name = name;
    this.entity = entity;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, name, entity);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.getOpenStreetMapLink(), name, entity);
  }
}
