package org.opentripplanner.graph_builder.module.ned;

import com.google.common.io.ByteStreams;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A coverage factory that works off of the NED caches from {@link NEDDownloader}.
 */
public class NEDGridCoverageFactoryImpl implements ElevationGridCoverageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NEDGridCoverageFactoryImpl.class);

    private final File cacheDirectory;

    public final NEDTileSource tileSource;

    private List<VerticalDatum> datums;

    private List<GridCoverage2D> regionCoverages = new ArrayList<>();

    public NEDGridCoverageFactoryImpl(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        this.tileSource = new NEDDownloader();
    }

    public NEDGridCoverageFactoryImpl(File cacheDirectory, NEDTileSource tileSource) {
        this.cacheDirectory = cacheDirectory;
        this.tileSource = tileSource;
    }

    private static final String[] DATUM_FILENAMES = {"g2012a00.gtx", "g2012g00.gtx", "g2012h00.gtx", "g2012p00.gtx", "g2012s00.gtx", "g2012u00.gtx"};

    /*
     * Summarizing from http://www.nauticalcharts.noaa.gov/csdl/learn_datum.html:
     * Like GPS, OpenStreetMap uses the World Geodetic System of 1984 (WGS84) coordinate system, 
     * and altitudes in OSM are measured relative to the WGS84 datum. On the other hand, USGS 
     * elevation data from the National Elevation Dataset (NED, http://ned.usgs.gov/) are 
     * referenced to the North American Vertical Datum of 1988 (NAVD88).
     * The NAVD88 datum used by NED is an "orthometric" datum based on mean sea level in one 
     * particular part of the world; the so-called 3D datums used in GPS and OSM are ellipsoids 
     * intended to cover the whole Earth. Orthometric datums like NAVD 88 are equipotential 
     * (gravitational) surfaces of the Earth (geoids [1]) which include the effects of topography 
     * because the Earth's mass is irregularly distributed. Ellipsoid datums like NAD83 are smooth 
     * geometric approximations of the earth’s surface (ellipsoids) without topography. 
     * Differences between the two are significant (up to 100 meters).
     * 
     * Current geoid models relate NAD83 ellipsoid heights to NAVD88 orthometric heights, i.e. 
     * the geoid for the continental United States is calibrated against and defined relative to 
     * the GPS ellipsoid. 
     * 
     * According to http://www.profsurv.com/magazine/article.aspx?i=561, "it is generally assumed 
     * that WGS 84 (original) is identical to NAD 83 (1986)." 
     * According to http://www.nauticalcharts.noaa.gov/csdl/learn_datum.html "there is a 2 meter 
     * difference between two of the most frequently used 3-D datums, the North American Datum of 
     * 1983 (NAD 83) and the World Geodetic System of 1984 (WGS 84)." 
     * 
     * In OTP we convert between these two systems using one of these geoids defined relative 
     * to an ellipsoid. The rasters describing the datum are not included in OTP by default because 
     * they double the size of the OTP distribution, but are only needed by people loading 
     * elevations in North America. 
     * 
     * In OTP we perform the conversion using a geoid defined relative to the NAD83 ellipsoid. 
     * This is backed up by an NOAA publication at 
     * http://www.ngs.noaa.gov/PUBS_LIB/FedRegister/FRdoc95-19408.pdf stating they are for all 
     * practical purposes identical, especially when using handheld equipment. NAD 83 and WGS 84 
     * ellipsoid equivalence is also explained in a post at 
     * http://forums.groundspeak.com/GC/index.php?showtopic=97337.
     * 
     * The datum rasters must be downloaded from the OTP website and placed in the NED cache directory.
     * TODO they could be fetched automatically from a static URL on the opentripplanner website.
     */
    private void loadVerticalDatum () {
        if (datums == null) {
            datums = new ArrayList<VerticalDatum>();
            try {
                for (String filename : DATUM_FILENAMES) {
                    File datumFile = new File(cacheDirectory, filename);
                    VerticalDatum datum = VerticalDatum.fromGTX(new FileInputStream(datumFile)); 
                    datums.add(datum);
                }
            } catch (IOException e) {
                LOG.error("Datum file has disappeared since preflight inputs check.");
                throw new RuntimeException(e);
            }            
        }
    }

    /**
     * Creates a new thread-specific UnifiedGridCoverage instance with new Interpolator2D instances that wrap the
     * underlying shared elevation tile data. During a refactor in the year 2020, the code at one point was written such
     * that a new UnifiedGridCoverage instance was created with unique tile data for each thread to use. However,
     * benchmarking showed that this caused longer run times which is likely due to too much memory competing for a slot
     * in the processor cache.
     */
    public Coverage getGridCoverage() {
        // If the tile data hasn't been loaded into memory yet, do that now.
        if (regionCoverages.size() == 0) {
            loadVerticalDatum();
            // Make one grid coverage for each NED tile, adding them to a list of coverage instances that can then be
            // wrapped with thread-specific interpolators.
            for (File path : tileSource.getNEDTiles()) {
                GeotiffGridCoverageFactoryImpl factory = new GeotiffGridCoverageFactoryImpl(path);
                regionCoverages.add(factory.getUninterpolatedGridCoverage());
            }
        }

        // Create a new UnifiedGridCoverage using the shared region coverages.
        return new UnifiedGridCoverage(regionCoverages, datums);
    }

    /**
     * Grab the rather voluminous vertical datum files from the OTP web server and save them in the NED cache directory.
     */
    private void fetchDatum() throws Exception {
        LOG.info("Attempting to fetch datum files from OTP project web server...");
        URL datumUrl = new URL("http://dev.opentripplanner.org/resources/datum.zip");
        ZipInputStream zis = new ZipInputStream(datumUrl.openStream());
        /* Silly boilerplate because Java has no simple unzip-to-directory function. */
        for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
            if (entry.isDirectory()) {
                throw new RuntimeException("ZIP files containing directories are not supported");
            }
            File file = new File(cacheDirectory, entry.getName());
            if (!file.getParentFile().equals(cacheDirectory)) {
                throw new RuntimeException("ZIP files containing directories are not supported");
            }
            LOG.info("decompressing {}", file);
            OutputStream os = new FileOutputStream(file);
            ByteStreams.copy(zis, os);
            os.close();
        }
        zis.close();
        LOG.info("Done.");
    }

    @Override
    public void checkInputs() {
        /* Attempt to create cache directory if it doesn't exist. */
        if (!cacheDirectory.exists()) {
            LOG.info("Cache directory {} does not exist, creating it.", cacheDirectory);
            if (!cacheDirectory.mkdirs()) {
                throw new RuntimeException("Failed to create cache directory for NED at " + cacheDirectory);
            }
        }
        if (!cacheDirectory.canRead() || !cacheDirectory.canWrite()) {
            throw new RuntimeException(String.format("Can't write and write NED cache at '%s'. Check permissions.", cacheDirectory));
        }
        boolean missingDatum = false;
        for (String filename : DATUM_FILENAMES) {
            File datumFile = new File(cacheDirectory, filename);
            if (! datumFile.canRead()) {
                missingDatum = true;
            }
        }
        if (missingDatum) {
            /* Attempt to fetch the datum files from the web. */
            LOG.warn("OTP needs additional files (a vertical datum) to convert between NED elevations and OSM's WGS84 elevations.");
            try {
                fetchDatum();
            } catch (Exception ex) {
                LOG.error("Exception while fetching datum files from the web.");
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Verify that the needed elevation data exists in the cache and if it does not exist, try to download it. The graph
     * is used to determine the extent of the NED.
     */
    @Override
    public void fetchData(Graph graph) {
        tileSource.fetchData(graph, cacheDirectory);
    }
}