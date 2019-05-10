package org.opentripplanner.gtfs;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.opentripplanner.graph_builder.module.GtfsFeedId;

import java.io.File;
import java.io.IOException;

class GtfsImport {

    private GtfsFeedId feedId = null;

    private GtfsMutableRelationalDao dao = null;

    GtfsImport(String defaultFeedId, File path) throws IOException {
        GtfsReader reader = new GtfsReader();
        reader.setInputLocation(path);

        if(defaultFeedId != null) {
            reader.setDefaultAgencyId(defaultFeedId);
        }
        readFeedId(defaultFeedId, reader);
        readDao(reader);
    }

    GtfsMutableRelationalDao getDao() {
        return dao;
    }

    GtfsFeedId getFeedId() {
        return feedId;
    }


    /* private methods */

    private void readDao(GtfsReader reader) throws IOException {
        dao = new GtfsRelationalDaoImpl();
        reader.setEntityStore(dao);
        reader.setDefaultAgencyId(getFeedId().getId());
        reader.run();
    }

    private void readFeedId(String defaultFeedId, GtfsReader reader) {
        if(defaultFeedId == null) {
            feedId = new GtfsFeedId.Builder().fromGtfsFeed(reader.getInputSource()).build();
        }
        else {
            feedId = new GtfsFeedId.Builder().id(defaultFeedId).build();
        }
    }

}
