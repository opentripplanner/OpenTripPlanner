package org.opentripplanner.ext.reportapi.model;

import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;

public class BicycleSafetyReport {

  public static void main(String[] args) {
    System.out.println(makeCsv(OsmTagMapperSource.NORWAY));
  }

  public static String makeCsv(OsmTagMapperSource source) {
    var wayPropertySet = new WayPropertySet();

    source.getInstance().populateProperties(wayPropertySet);

    var buf = new CsvReportBuilder(",");

    buf.addHeader(
      "OSM tags for osmWayPropertySet " + source,
      "mixin",
      "permissions",
      "safety penalty there",
      "safety penalty back"
    );

    wayPropertySet
      .getWayProperties()
      .forEach(p -> {
        buf.addText(p.specifier().toString());
        buf.addBoolean(false);
        buf.addText(p.properties().getPermission().toString());

        var safetyProps = p.properties().bicycleSafety();
        if (safetyProps != null) {
          buf.addNumber(safetyProps.forward());
          buf.addNumber(safetyProps.back());
        }
        buf.newLine();
      });

    wayPropertySet
      .getMixins()
      .forEach(p -> {
        buf.addText(p.specifier().toString());
        buf.addBoolean(true);
        buf.addText("");

        var safetyProps = p.bicycleSafety();
        buf.addNumber(safetyProps.forward());
        buf.addNumber(safetyProps.back());
        buf.newLine();
      });

    return buf.toString();
  }
}
