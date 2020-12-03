package org.opentripplanner.standalone.config;

import com.csvreader.CsvReader;
import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubmodesConfig {

  private static final String DEFAULT_FILE = "org/opentripplanner/submodes/submodes.csv";

  private static final Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;

  private static final char CSV_DELIMITER = ',';

  private static final String LIST_DELIMITER = " ";

  private final List<ConfigItem> configItems = new ArrayList<>();

  private static final Logger LOG = LoggerFactory.getLogger(SubmodesConfig.class);

  public SubmodesConfig(InputStream inputStream) {
    try {
      CsvReader csvReader = new CsvReader(inputStream, CSV_DELIMITER, CHARSET_UTF_8);
      csvReader.readHeaders(); // Skip header
      while (csvReader.readRecord()) {
        configItems.add(new ConfigItem(csvReader.get("name"),
            TransitMainMode.valueOf(csvReader.get("mode")),
            csvReader.get("description"),
            asList(csvReader.get("netexSubmodes")),
            asList(csvReader.get("gtfsExtendedRouteTypes")),
            csvReader.get("netexOutputSubmode"),
            csvReader.get("gtfsOutputExtendedRouteType")
        ));
      }
    }
    catch (NullPointerException | IOException e) {
      LOG.error("Could not read submodes from file", e);
    }
  }

  public static SubmodesConfig getDefault() {
    return new SubmodesConfig((SubmodesConfig.class.getClassLoader().getResourceAsStream(DEFAULT_FILE)));
  }

  public List<TransitMode> getSubmodes() {
    return configItems
        .stream()
        .map(c -> new TransitMode(c.mode,
            c.name,
            c.description,
            c.netexSubmodes,
            c.gtfsExtendRouteTypes,
            c.netexOutputSubmode,
            c.gtfsOutputExtendedRouteType
        ))
        .collect(Collectors.toList());
  }

  private static class ConfigItem {

    public final String name;
    public final TransitMainMode mode;
    public final String description;
    public final List<String> netexSubmodes;
    public final List<String> gtfsExtendRouteTypes;
    public final String netexOutputSubmode;
    public final String gtfsOutputExtendedRouteType;

    public ConfigItem(
        String name, TransitMainMode mode, String description, List<String> netexSubmodes,
        List<String> gtfsExtendRouteTypes, String netexOutputSubmode, String gtfsOutputExtendedRouteType
    ) {
      this.name = name;
      this.mode = mode;
      this.description = description;
      this.netexSubmodes = netexSubmodes;
      this.gtfsExtendRouteTypes = gtfsExtendRouteTypes;
      this.netexOutputSubmode = netexOutputSubmode;
      this.gtfsOutputExtendedRouteType = gtfsOutputExtendedRouteType;
    }
  }

  private List<String> asList(String input) {
    return Arrays.asList(input.split(LIST_DELIMITER));
  }
}
