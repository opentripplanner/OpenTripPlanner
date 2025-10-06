package org.opentripplanner.gtfs.graphbuilder;

import java.io.File;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.DataSourceTestFactory;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.gtfs.config.GtfsDefaultParameters;

public class GtfsBundleTestFactory {

  public static GtfsBundle forTest(File gtfsFile, @Nullable String feedId) {
    var dataSource = DataSourceTestFactory.compositeSource(gtfsFile, FileType.GTFS);
    var parameters = GtfsDefaultParameters.DEFAULT.withFeedInfo()
      .withSource(dataSource.uri())
      .withFeedId(feedId)
      .build();
    return new GtfsBundle(dataSource, parameters);
  }

  public static GtfsBundle forTest(File gtfsFile) {
    return forTest(gtfsFile, null);
  }
}
