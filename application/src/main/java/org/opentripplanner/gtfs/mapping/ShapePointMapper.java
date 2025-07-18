package org.opentripplanner.gtfs.mapping;

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
      .forEach(row -> {
        var shapeId = idFactory.createId(row.string(SHAPE_ID), "shape point");
        var shapeBuilder = ret.getOrDefault(shapeId, new CompactShape());

        var point = new ShapePoint();
        point.setSequence(row.integer(SHAPE_PT_SEQUENCE));
        point.setLat(row.doubble(SHAPE_PT_LAT));
        point.setLon(row.doubble(SHAPE_PT_LON));

        row.optionalDouble(SHAPE_DIST_TRAVELED).ifPresent(point::setDistTraveled);

        shapeBuilder.addPoint(point);
        ret.put(shapeId, shapeBuilder);
      });

    return ret;
  }
}
