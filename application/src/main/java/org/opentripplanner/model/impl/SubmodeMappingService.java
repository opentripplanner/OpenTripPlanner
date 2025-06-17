package org.opentripplanner.model.impl;

import com.csvreader.CsvReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class SubmodeMappingService {

  private static final String INPUT_FEED_TYPE = "Input feed type";
  private static final String INPUT_LABEL = "Input label";
  private static final String GTFS_ROUTE_TYPE = "GTFS route type";
  private static final String NETEX_SUBMODE = "NeTEx submode";
  private static final String REPLACEMENT_MODE = "Replacement mode";

  public record SubmodeMappingMatcher(String inputFeedType, String inputLabel) {}

  public record SubmodeMappingRow(
    int gtfsRouteType,
    String netexSubmode,
    TransitMode replacementMode
  ) {}

  private final Map<SubmodeMappingMatcher, SubmodeMappingRow> map;

  public SubmodeMappingService(@Nullable String filename) {
    map = new HashMap<>();
    if (filename != null) {
      read(filename);
    } else {
      useDefaultMapping();
    }
  }

  public Optional<SubmodeMappingRow> mapGtfsExtendedType(int extendedType) {
    return Optional.ofNullable(
      map.get(new SubmodeMappingMatcher("GTFS", Integer.toString(extendedType)))
    );
  }

  public Optional<SubmodeMappingRow> mapNetexSubmode(SubMode submode) {
    return Optional.ofNullable(map.get(new SubmodeMappingMatcher("NeTEx", submode.toString())));
  }

  public void read(String filename) {
    try {
      var inputStream = new FileInputStream(filename);
      var reader = new CsvReader(inputStream, StandardCharsets.UTF_8);
      reader.readHeaders();
      var headers = reader.getHeaders();
      while (reader.readRecord()) {
        var inputFeedType = reader.get(INPUT_FEED_TYPE);
        var inputLabel = reader.get(INPUT_LABEL);
        var gtfsRouteType = reader.get(GTFS_ROUTE_TYPE);
        var netexSubmode = reader.get(NETEX_SUBMODE);
        var replacementMode = reader.get(REPLACEMENT_MODE);
        var matcher = new SubmodeMappingMatcher(inputFeedType, inputLabel);
        var row = new SubmodeMappingRow(
          Integer.parseInt(gtfsRouteType),
          netexSubmode,
          TransitMode.valueOf(replacementMode)
        );
        this.map.put(matcher, row);
      }
    } catch (IOException ioe) {
      throw new OtpAppException("cannot read submode mapping config file " + filename, ioe);
    }
  }

  public void useDefaultMapping() {
    map.put(
      new SubmodeMappingMatcher("GTFS", "714"),
      new SubmodeMappingRow(714, "railReplacementBus", TransitMode.RAIL)
    );
    map.put(
      new SubmodeMappingMatcher("NeTEx", "railreplacementBus"),
      new SubmodeMappingRow(714, "railReplacementBus", TransitMode.RAIL)
    );
  }
}
