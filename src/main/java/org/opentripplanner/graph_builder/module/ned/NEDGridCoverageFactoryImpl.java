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

package org.opentripplanner.graph_builder.module.ned;

import com.google.common.io.ByteStreams;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.InterpolationBilinear;
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

    private Graph graph;

    /** All tiles for the DEM stitched into a single coverage. */
    UnifiedGridCoverage unifiedCoverage = null;

    private File cacheDirectory;

    public NEDTileSource tileSource = new NEDDownloader();

    private List<VerticalDatum> datums;

    public NEDGridCoverageFactoryImpl(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
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

    /** @return a GeoTools grid coverage for the entire area of interest, lazy-creating it on the first call. */
    public Coverage getGridCoverage() {
        if (unifiedCoverage == null) {
            loadVerticalDatum();
            tileSource.setGraph(graph);
            tileSource.setCacheDirectory(cacheDirectory);
            List<File> paths = tileSource.getNEDTiles();
            // Make one grid coverage for each NED tile, adding them all to a single UnifiedGridCoverage.
            for (File path : paths) {
                GeotiffGridCoverageFactoryImpl factory = new GeotiffGridCoverageFactoryImpl(path);
                // TODO might bicubic interpolation give better results?
                GridCoverage2D regionCoverage = Interpolator2D.create(factory.getGridCoverage(),
                        new InterpolationBilinear());
                if (unifiedCoverage == null) {
                    unifiedCoverage = new UnifiedGridCoverage("unified", regionCoverage, datums);
                } else {
                    unifiedCoverage.add(regionCoverage);
                }
            }
        }
        return unifiedCoverage;
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

    /** Set the graph that will be used to determine the extent of the NED. */
    @Override
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}