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

package org.opentripplanner.graph_builder.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import org.apache.http.client.ClientProtocolException;
import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.csv_entities.FileCsvInputSource;
import org.onebusaway.csv_entities.ZipFileCsvInputSource;
import org.opentripplanner.graph_builder.module.DownloadableGtfsInputSource;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsBundle {

    private static final Logger LOG = LoggerFactory.getLogger(GtfsBundle.class);

    private File path;

    private URL url;

    private GtfsFeedId feedId;

    private CsvInputSource csvInputSource;

    private Boolean defaultBikesAllowed = true;

    private boolean transfersTxtDefinesStationPaths = false;

    /** 
     * Create direct transfers between the constituent stops of each parent station.
     * This is different from "linking stops to parent stations" below.
     */
    public boolean parentStationTransfers = false;

    /** 
     * Connect parent station vertices to their constituent stops to allow beginning and 
     * ending paths (itineraries) at them. 
     */
    public boolean linkStopsToParentStations = false;

    private Map<String, String> agencyIdMappings = new HashMap<String, String>();

    public int subwayAccessTime;

    private double maxStopToShapeSnapDistance = 150;

    public Boolean useCached = null; // null means use global default from GtfsGB || true

    public File cacheDirectory = null; // null means use default from GtfsGB || system temp dir 

    public GtfsBundle() {
    }
    
    public GtfsBundle(File gtfsFile) {
        this.setPath(gtfsFile);
    }

    public void setPath(File path) {
        this.path = path;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setCsvInputSource(CsvInputSource csvInputSource) {
        this.csvInputSource = csvInputSource;
    }
    
    public String getDataKey() {
        return path + ";" + url + ";" + (csvInputSource != null ? csvInputSource.hashCode() : "");
    }
    
    public CsvInputSource getCsvInputSource() throws IOException {
        if (csvInputSource == null) {
            if (path != null) {
                if (path.isDirectory()) {
                    csvInputSource = new FileCsvInputSource(path);
                } else {
                    csvInputSource = new ZipFileCsvInputSource(new ZipFile(path));
                }
            } else if (url != null) {
                DownloadableGtfsInputSource isrc = new DownloadableGtfsInputSource();
                isrc.setUrl(url);
                if (cacheDirectory != null)
                    isrc.setCacheDirectory(cacheDirectory);
                if (useCached != null)
                    isrc.useCached = useCached;
                csvInputSource = isrc;
            }
        }
        return csvInputSource;
    }

    public String toString () {
        String src; 
        if (path != null) {
            src = path.toString();
        } else if (url != null) {
            src = url.toString();
        } else {
            src = "(no source)";
        }
        if (feedId != null) {
            src += " (" + feedId.getId() + ")";
        }
        return "GTFS bundle at " + src;
    }
    
    /**
     * So that we can load multiple gtfs feeds into the same database.
     */
    public GtfsFeedId getFeedId() {
        if (feedId == null) {
            try {
                feedId = new GtfsFeedId.Builder().fromGtfsFeed(getCsvInputSource()).build();
            } catch (IOException e) {
                LOG.error("Failed to fetch feedId from feed_info.");
                throw new RuntimeException(e);
            }
        }
        return feedId;
    }

    public void setFeedId(GtfsFeedId feedId) {
        this.feedId = feedId;
    }

    public Map<String, String> getAgencyIdMappings() {
        return agencyIdMappings;
    }

    public void setAgencyIdMappings(Map<String, String> agencyIdMappings) {
        this.agencyIdMappings = agencyIdMappings;
    }

    /**
     * When a trip doesn't contain any bicycle accessibility information, should taking a bike
     * along a transit trip be permitted?
     * A trip doesn't contain bicycle accessibility information if both route_short_name and
     * trip_short_name contain missing/0 values.
     */
    public Boolean getDefaultBikesAllowed() {
        return defaultBikesAllowed;
    }

    public void setDefaultBikesAllowed(Boolean defaultBikesAllowed) {
        this.defaultBikesAllowed = defaultBikesAllowed;
    }

    /**
     * Transfers.txt usually specifies where the transit operator prefers people to transfer, 
     * due to schedule structure and other factors.
     * 
     * However, in systems like the NYC subway system, transfers.txt can partially substitute 
     * for the missing pathways.txt file.  In this case, transfer edges will be created between
     * stops where transfers are defined.
     * 
     * @return
     */
    public boolean doesTransfersTxtDefineStationPaths() {
        return transfersTxtDefinesStationPaths;
    }

    public void setTransfersTxtDefinesStationPaths(boolean transfersTxtDefinesStationPaths) {
        this.transfersTxtDefinesStationPaths = transfersTxtDefinesStationPaths;
    }

    public void checkInputs() {
        if (csvInputSource != null) {
            LOG.warn("unknown CSV source type; cannot check inputs");
            return;
        }
        if (path != null) {
            if (!path.exists()) {
                throw new RuntimeException("GTFS Path " + path + " does not exist.");
            }
            if (!path.canRead()) {
                throw new RuntimeException("GTFS Path " + path + " cannot be read.");
            }
        } else if (url != null) {
            try {
                HttpUtils.testUrl(url.toExternalForm());
            } catch (ClientProtocolException e) {
                throw new RuntimeException("Error connecting to " + url.toExternalForm() + "\n" + e);
            } catch (IOException e) {
                throw new RuntimeException("GTFS url " + url.toExternalForm() + " cannot be read.\n" + e);
            }
        }

    }

    public double getMaxStopToShapeSnapDistance() {
        return maxStopToShapeSnapDistance;
    }

    public void setMaxStopToShapeSnapDistance(double maxStopToShapeSnapDistance) {
        this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
    }
}
