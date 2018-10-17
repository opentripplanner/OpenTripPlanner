package org.opentripplanner.gtfs;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.opentripplanner.graph_builder.module.GtfsFeedId;

import java.io.File;
import java.io.IOException;

public class GtfsImport {


    private GtfsFeedId feedId = null;

    private GtfsRelationalDaoImpl dao = null;

    public GtfsImport(File path) throws IOException {
        GtfsReader reader = new GtfsReader();
        reader.setInputLocation(path);
        readFeedId(reader);
        readDao(reader);
    }

    public GtfsMutableRelationalDao getDao() {
        return dao;
    }

    public GtfsFeedId getFeedId() {
        return feedId;
    }


    /* private methods */

    private void readDao(GtfsReader reader) throws IOException {
        dao = new GtfsRelationalDaoImpl();
        reader.setEntityStore(dao);
        reader.setDefaultAgencyId(getFeedId().getId());
        reader.run();
    }

    private void readFeedId(GtfsReader reader) {
        feedId = new GtfsFeedId.Builder().fromGtfsFeed(reader.getInputSource()).build();
    }

}
