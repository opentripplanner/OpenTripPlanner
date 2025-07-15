package org.opentripplanner.gtfs.mapping;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.units.qual.C;
import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.gtfs.model.ShapePoint;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/** Responsible for mapping GTFS ShapePoint into the OTP model. */
class ShapePointMapper {

  private static final String FILE = "shapes.txt";
  private static final String SHAPE_DIST_TRAVELED = "shape_dist_traveled";
  private static final String SHAPE_ID = "shape_id";
  private static final String SHAPE_PT_SEQUENCE = "shape_pt_sequence";
  private static final String SHAPE_PT_LAT = "shape_pt_lat";
  private static final String SHAPE_PT_LON = "shape_pt_lon";
  private final IdFactory idFactory;

  ShapePointMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Map<FeedScopedId, CompactShape> map(CsvInputSource inputSource) throws IOException {
    var ret = new HashMap<FeedScopedId, CompactShape>();
    new StreamingCsvReader(inputSource)
      .rows(FILE)
      .forEach(sp -> {
        var shapeId = idFactory.createId(sp.get(SHAPE_ID), "shape point");
        var shapeBuilder = ret.getOrDefault(shapeId, new CompactShape());

        var point = new ShapePoint();
        point.setSequence(parseInt(sp.get(SHAPE_PT_SEQUENCE)));
        point.setLat(parseDouble(sp.get(SHAPE_PT_LAT)));
        point.setLon(parseDouble(sp.get(SHAPE_PT_LON)));

        var distTraveled = sp.get(SHAPE_DIST_TRAVELED);
        if (distTraveled != null) {
          point.setDistTraveled(parseDouble(distTraveled));
        }

        shapeBuilder.addPoint(point);
        ret.put(shapeId, shapeBuilder);
      });

    return ret;
  }
}
