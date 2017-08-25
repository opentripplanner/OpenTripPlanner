/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.gtfs;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.opentripplanner.graph_builder.module.GtfsFeedId;

import java.io.File;
import java.io.IOException;

class GtfsImport {

    private final GtfsReader reader;
    private GtfsFeedId feedId = null;
    private GtfsRelationalDaoImpl dao = null;

    GtfsImport(File path) throws IOException {
        this.reader = new GtfsReader();
        reader.setInputLocation(path);
    }

    /** TODO TGR - This does not need to be mutable? */
    GtfsMutableRelationalDao getDao() throws IOException {
        if(dao == null) {
            dao = new GtfsRelationalDaoImpl();
            reader.setEntityStore(dao);
            reader.setDefaultAgencyId(getFeedId().getId());
            reader.run();
        }
        return dao;
    }

    GtfsFeedId getFeedId() {
        if(feedId == null) {
            feedId = new GtfsFeedId.Builder().fromGtfsFeed(reader.getInputSource()).build();
        }
        return feedId;
    }
}
