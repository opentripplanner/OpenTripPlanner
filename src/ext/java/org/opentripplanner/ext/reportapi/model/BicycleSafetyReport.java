package org.opentripplanner.ext.reportapi.model;

import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;

public class BicycleSafetyReport {

  public static void main(String[] args) {
    System.out.println(makeCsv(OsmTagMapper.Source.NORWAY));
  }

  public static String makeCsv(OsmTagMapper.Source source) {
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
        buf.addBoolean(p.safetyMixin());
        buf.addText(p.properties().getPermission().toString());

        var safetyProps = p.properties().getBicycleSafetyFeatures();
        buf.addNumber(safetyProps.forward());
        buf.addNumber(safetyProps.back());
        buf.newLine();
      });

    return buf.toString();
  }
}
