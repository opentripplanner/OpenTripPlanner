package org.opentripplanner.gtfs.graphbuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import javax.annotation.Nullable;
import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Area;
import org.onebusaway.gtfs.model.FareLegRule;
import org.onebusaway.gtfs.model.FareMedium;
import org.onebusaway.gtfs.model.FareProduct;
import org.onebusaway.gtfs.model.FareTransferRule;
import org.onebusaway.gtfs.model.RiderCategory;
import org.onebusaway.gtfs.model.RouteNetworkAssignment;
import org.onebusaway.gtfs.model.StopAreaElement;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GtfsBundle {

  private static final Set<Class<?>> FARES_V2_CLASSES = Set.of(
    Area.class,
    FareProduct.class,
    FareLegRule.class,
    FareMedium.class,
    FareTransferRule.class,
    RiderCategory.class,
    RouteNetworkAssignment.class,
    StopAreaElement.class
  );

  private static final Logger LOG = LoggerFactory.getLogger(GtfsBundle.class);

  private final CompositeDataSource dataSource;

  // The feedId is resolved lazy to make any exceptions in the caller when parsing the
  // gfts files, and not in the instrumentation of the bundle.
  @Nullable
  private String feedId;

  private CsvInputSource csvInputSource;

  private final GtfsFeedParameters parameters;

  public GtfsBundle(CompositeDataSource dataSource, GtfsFeedParameters parameters) {
    this.dataSource = dataSource;
    this.parameters = parameters;
    // Override feed id, if set in config
    this.feedId = parameters.feedId();
  }

  /**
   * So that we can load multiple gtfs feeds into the same database.
   */
  public String getFeedId() {
    if (feedId == null) {
      feedId = GtfsFeedIdResolver.fromGtfsFeed(getCsvInputSource(), dataSource.uri());
    }
    return feedId;
  }

  public GtfsFeedParameters parameters() {
    return parameters;
  }

  public void checkInputs() {
    if (csvInputSource != null) {
      LOG.warn("unknown CSV source type; cannot check inputs");
      return;
    }
    if (!dataSource.exists()) {
      throw new RuntimeException(
        "GTFS Path " + dataSource.path() + " does not exist or " + "cannot be read."
      );
    }
  }

  public CsvInputSource getCsvInputSource() {
    if (csvInputSource == null) {
      csvInputSource = new CsvInputSource() {
        @Override
        public boolean hasResource(String s) {
          return dataSource
            .content()
            .stream()
            .anyMatch(it -> it.name().equals(s));
        }

        @Override
        public InputStream getResource(String s) {
          return dataSource.entry(s).asInputStream();
        }

        @Override
        public void close() {}
      };
    }
    return csvInputSource;
  }

  public String feedInfo() {
    return "GTFS bundle at " + dataSource.path() + " (" + getFeedId() + ")";
  }

  public GtfsRelationalDao loadDao() throws IOException {
    var dao = new GtfsRelationalDaoImpl();
    dao.setPackShapePoints(true);
    LOG.info("reading {}", feedInfo());

    GtfsReader reader = new GtfsReader();
    reader.setInputSource(getCsvInputSource());
    reader.setEntityStore(dao);
    reader.setInternStrings(true);
    reader.setDefaultAgencyId(getFeedId());

    dao.open();
    for (Class<?> entityClass : reader.getEntityClasses()) {
      if (skipEntityClass(entityClass)) {
        LOG.info("Skipping entity: {}", entityClass.getName());
        continue;
      }
      LOG.info("Reading entity: {}", entityClass.getName());
      reader.readEntities(entityClass);
    }
    dao.close();
    return dao;
  }

  /**
   * Since GTFS Fares V2 is a very new, constantly evolving standard there might be a lot of errors
   * in the data. We only want to try to parse them when the feature flag is explicitly enabled as
   * it can easily lead to graph build failures.
   */
  private boolean skipEntityClass(Class<?> entityClass) {
    return OTPFeature.FaresV2.isOff() && FARES_V2_CLASSES.contains(entityClass);
  }
}
