package org.opentripplanner.model.impl;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.model.FeedType;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * Part of infra to map GTFS and NeTEx Trip.replacementMode similarly.
 *
 * @see SubmodeMappingService
 */
public class SubmodeMappingModule implements GraphBuilderModule {

  private static final String INPUT_FEED_TYPE = "Input feed type";
  private static final String INPUT_LABEL = "Input label";
  private static final String NETEX_SUBMODE = "NeTEx submode";
  private static final String REPLACEMENT_MODE = "Replacement mode";
  private static final String ORIGINAL_MODE = "Original mode";
  private static final String GTFS_REPLACEMENT_MODE = "GTFS replacement mode";
  private static final String GTFS_REPLACEMENT_TYPE = "GTFS replacement type";
  private static final String[] MANDATORY = { INPUT_FEED_TYPE, INPUT_LABEL };

  private final TimetableRepository timetableRepository;

  @Nullable
  private final DataSource dataSource;

  public SubmodeMappingModule(
    GraphBuilderDataSources graphBuilderDataSources,
    TimetableRepository timetableRepository
  ) {
    this.timetableRepository = timetableRepository;
    this.dataSource = graphBuilderDataSources.getSubmodeMappingDataSource().orElse(null);
  }

  private boolean isEmpty(@Nullable String string) {
    return string == null || string.isEmpty();
  }

  private Map<SubmodeMappingMatcher, SubmodeMappingRow> read(DataSource dataSource) {
    var map = new HashMap<SubmodeMappingMatcher, SubmodeMappingRow>();
    try {
      var reader = new CsvReader(dataSource.asInputStream(), StandardCharsets.UTF_8);
      reader.readHeaders();
      var headers = reader.getHeaders();
      for (var header : MANDATORY) {
        if (Arrays.stream(headers).noneMatch(h -> h.equals(header))) {
          throw new OtpAppException("submode mapping header not found: " + header);
        }
      }
      while (reader.readRecord()) {
        var inputFeedType = FeedType.of(reader.get(INPUT_FEED_TYPE));
        if (inputFeedType == null) {
          throw new OtpAppException(
            "not a valid submode mapping feed type: " + reader.get(INPUT_FEED_TYPE)
          );
        }
        var inputLabel = reader.get(INPUT_LABEL);
        var netexSubmode = reader.get(NETEX_SUBMODE);
        var replacementMode = isEmpty(reader.get(REPLACEMENT_MODE))
          ? null
          : TransitMode.valueOf(reader.get(REPLACEMENT_MODE));
        var originalMode = isEmpty(reader.get(ORIGINAL_MODE))
          ? null
          : TransitMode.valueOf(reader.get(ORIGINAL_MODE));
        var gtfsReplacementMode = isEmpty(reader.get(GTFS_REPLACEMENT_MODE))
          ? null
          : TransitMode.valueOf(reader.get(GTFS_REPLACEMENT_MODE));
        var gtfsReplacementType = isEmpty(reader.get(GTFS_REPLACEMENT_TYPE))
          ? null
          : Integer.parseInt(reader.get(GTFS_REPLACEMENT_TYPE));
        var matcher = new SubmodeMappingMatcher(inputFeedType, inputLabel);
        var row = new SubmodeMappingRow(
          netexSubmode,
          replacementMode,
          originalMode,
          gtfsReplacementMode,
          gtfsReplacementType
        );
        map.put(matcher, row);
      }
    } catch (IOException ioe) {
      throw new OtpAppException("cannot read submode mapping config file", ioe);
    }
    return map;
  }

  private Map<SubmodeMappingMatcher, SubmodeMappingRow> useDefaultMapping() {
    var map = new HashMap<SubmodeMappingMatcher, SubmodeMappingRow>();
    map.put(
      new SubmodeMappingMatcher(FeedType.GTFS, "714"),
      new SubmodeMappingRow("railReplacementBus", null, TransitMode.RAIL, null, null)
    );
    map.put(
      new SubmodeMappingMatcher(FeedType.NETEX, "railReplacementBus"),
      new SubmodeMappingRow("railReplacementBus", TransitMode.BUS, null, TransitMode.BUS, 714)
    );
    return map;
  }

  @Override
  public void buildGraph() {
    if (dataSource != null) {
      timetableRepository.setSubmodeMapping(read(dataSource));
    } else {
      timetableRepository.setSubmodeMapping(useDefaultMapping());
    }
  }

  @Override
  public void checkInputs() {
    if (dataSource != null && !dataSource.exists()) {
      throw new RuntimeException(
        "Submode mapping file " + dataSource.path() + " does not exist or cannot be read."
      );
    }
  }
}
