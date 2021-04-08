package org.opentripplanner.graph_builder.model;

import org.apache.http.client.ClientProtocolException;
import org.onebusaway.csv_entities.CsvInputSource;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.configure.DataStoreFactory;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GtfsBundle {

    private static final Logger LOG = LoggerFactory.getLogger(GtfsBundle.class);

    private final CompositeDataSource dataSource;

    private URL url;

    private GtfsFeedId feedId;

    private CsvInputSource csvInputSource;

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

    public int maxInterlineDistance;

    public Boolean useCached = null; // null means use global default from GtfsGB || true

    public File cacheDirectory = null; // null means use default from GtfsGB || system temp dir 


    /** Used by unit tests */
    public GtfsBundle(File gtfsFile) {
        this(DataStoreFactory.compositeSource(gtfsFile, FileType.GTFS));
    }

    public GtfsBundle(CompositeDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public CsvInputSource getCsvInputSource() {
        if (csvInputSource == null) {
            csvInputSource = new CsvInputSource() {
                @Override
                public boolean hasResource(String s) {
                    return dataSource.content().stream().anyMatch(it -> it.name().equals(s));
                }

                @Override
                public InputStream getResource(String s) {
                    return dataSource.entry(s).asInputStream();
                }

                @Override
                public void close() { }
            };
        }
        return csvInputSource;
    }

    public void close() {
        try {
            dataSource.close();
        }
        catch (IOException e) {
            LOG.warn("Failed to close datasource {}, details: {}", dataSource.path(), e.getLocalizedMessage(), e);
        }
    }

    public String toString () {
        String src = dataSource.path();
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
            feedId = new GtfsFeedId.Builder().fromGtfsFeed(getCsvInputSource()).build();
        }
        return feedId;
    }

    public void setFeedId(GtfsFeedId feedId) {
        this.feedId = feedId;
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
        if (!dataSource.exists()) {
                throw new RuntimeException(
                        "GTFS Path " + dataSource.path() + " does not exist or "
                                + "cannot be read."
                );
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
