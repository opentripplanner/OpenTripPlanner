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

package org.opentripplanner.graph_builder.impl.ned;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.InterpolationBilinear;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.graph_builder.services.ned.NEDGridCoverageFactory;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A coverage factory that works off of the NED caches from {@link NEDDownloader}.
 */
public class NEDGridCoverageFactoryImpl implements NEDGridCoverageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NEDGridCoverageFactoryImpl.class);

    private Graph graph;

    UnifiedGridCoverage coverage = null;

    private File cacheDirectory;

    private NEDTileSource tileSource = new NEDDownloader();

    private List<VerticalDatum> datums;

    public NEDGridCoverageFactoryImpl () { }
    
    public NEDGridCoverageFactoryImpl(File cacheDirectory) {
        this.setCacheDirectory(cacheDirectory);
    }

    /** Set the directory where NED data will be cached. */
    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Autowired(required=false)
    public void setTileSource(NEDTileSource source) {
        this.tileSource = source;
    }

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
     */
    private void loadVerticalDatum () {
        if (datums == null) {
            datums = new ArrayList<VerticalDatum>();
            String[] datumFilenames = {"g2012a00.gtx","g2012g00.gtx","g2012h00.gtx","g2012p00.gtx","g2012s00.gtx","g2012u00.gtx"};
            try {
                for (String filename : datumFilenames) {
                    File datumFile = new File(cacheDirectory, filename);
                    VerticalDatum datum = VerticalDatum.fromGTX(new FileInputStream(datumFile)); 
                    datums.add(datum);
                }
            } catch (IOException e) {
                LOG.error("OTP needs additional files (a vertical datum) to convert between NED elevations and OSM's WGS84 elevations. See https://github.com/openplans/OpenTripPlanner/wiki/GraphBuilder#elevation-data for further information.");
                throw new RuntimeException(e);
            }            
        }
    }
    
    public Coverage getGridCoverage() {
        if (coverage == null) {
            loadVerticalDatum();
            tileSource.setGraph(graph);
            tileSource.setCacheDirectory(cacheDirectory);
            List<File> paths = tileSource.getNEDTiles();
            for (File path : paths) {
                GeotiffGridCoverageFactoryImpl factory = new GeotiffGridCoverageFactoryImpl();
                factory.setPath(path);
                GridCoverage2D regionCoverage = Interpolator2D.create(factory.getGridCoverage(),
                        new InterpolationBilinear());
                if (coverage == null) {
                    coverage = new UnifiedGridCoverage("unified", regionCoverage, datums);
                } else {
                    coverage.add(regionCoverage);
                }
            }
        }
        return coverage;
    }

    @Override
    public void checkInputs() {
        /* This is actually checking before we call tileSource.setCacheDirectory, which creates the dirs */
        if (!cacheDirectory.canWrite()) {
            throw new RuntimeException("Can't write to NED cache: " + cacheDirectory);
        }
    }

    /** Set the graph that will be used to determine the extent of the NED. */
    @Override
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}