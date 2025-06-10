package org.opentripplanner.gtfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedIdResolver;

class GtfsImport {

  private final String feedId;

  private GtfsMutableRelationalDao dao = null;

  GtfsImport(String defaultFeedId, File path) throws IOException {
    GtfsReader reader = new GtfsReader();
    reader.setInputLocation(path);

    if (defaultFeedId != null) {
      reader.setDefaultAgencyId(defaultFeedId);
    }
    this.feedId = resolveFeedId(defaultFeedId, reader, path.toURI());
    readDao(reader);
  }

  GtfsMutableRelationalDao getDao() {
    return dao;
  }

  String getFeedId() {
    return feedId;
  }

  /* private methods */

  private void readDao(GtfsReader reader) throws IOException {
    dao = new GtfsRelationalDaoImpl();
    reader.setEntityStore(dao);
    reader.setDefaultAgencyId(getFeedId());
    reader.run();
  }

  private static String resolveFeedId(String defaultFeedId, GtfsReader reader, URI uri) {
    return defaultFeedId == null
      ? GtfsFeedIdResolver.fromGtfsFeed(reader.getInputSource(), uri)
      : defaultFeedId;
  }
}
