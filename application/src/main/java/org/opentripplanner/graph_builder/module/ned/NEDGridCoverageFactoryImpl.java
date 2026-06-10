package org.opentripplanner.graph_builder.module.ned;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.geotools.api.coverage.Coverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.street.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A coverage factory that works off of the NED caches from NED tile sources.
 */
public class NEDGridCoverageFactoryImpl implements ElevationGridCoverageFactory {

  private static final Logger LOG = LoggerFactory.getLogger(NEDGridCoverageFactoryImpl.class);
  private static final String[] DATUM_FILENAMES = {
    "g2012a00.gtx",
    "g2012g00.gtx",
    "g2012h00.gtx",
    "g2012p00.gtx",
    "g2012s00.gtx",
    "g2012u00.gtx",
  };
  private final CompositeDataSource cacheDir;
  private final NEDTileSource tileSource;
  private final List<GridCoverage2D> regionCoverages = new ArrayList<>();
  private final URL datumUrl;
  private List<VerticalDatum> datums;

  public NEDGridCoverageFactoryImpl(
    CompositeDataSource cacheDir,
    URI datumUrl,
    NEDTileSource tileSource
  ) {
    this.cacheDir = cacheDir;
    this.tileSource = tileSource;
    try {
      this.datumUrl = datumUrl.toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new thread-specific UnifiedGridCoverage instance with new Interpolator2D instances
   * that wrap the underlying shared elevation tile data. During a refactor in the year 2020, the
   * code at one point was written such that a new UnifiedGridCoverage instance was created with
   * unique tile data for each thread to use. However, benchmarking showed that this caused longer
   * run times which is likely due to too much memory competing for a slot in the processor cache.
   */
  public Coverage getGridCoverage() {
    // If the tile data hasn’t been loaded into memory yet, do that now.
    if (regionCoverages.size() == 0) {
      loadVerticalDatum();
      // Make one grid coverage for each NED tile, adding them to a list of coverage instances that
      // can then be wrapped with thread-specific interpolators.
      for (DataSource tile : tileSource.getNEDTiles()) {
        GeotiffGridCoverageFactoryImpl factory = new GeotiffGridCoverageFactoryImpl(tile, 1.0);
        regionCoverages.add(factory.getUninterpolatedGridCoverage());
      }
    }

    // Create a new UnifiedGridCoverage using the shared region coverages.
    return new UnifiedGridCoverage(regionCoverages, datums);
  }

  @Override
  public double elevationUnitMultiplier() {
    // NED elevation is provided in decimal meter
    return 1;
  }

  @Override
  public void checkInputs() {
    if (!cacheDir.isWritable()) {
      throw new RuntimeException(
        "Can’t read/write NED cache at ‘" + cacheDir.path() + "’. Check permissions."
      );
    }
    boolean missingDatum = false;
    for (String filename : DATUM_FILENAMES) {
      if (!cacheDir.entry(filename).exists()) {
        missingDatum = true;
      }
    }
    if (missingDatum) {
      LOG.warn(
        "OTP needs additional files (a vertical datum) to convert between NED elevations and OSM’s WGS84 elevations."
      );
      try {
        fetchDatum();
      } catch (Exception ex) {
        LOG.error("Exception while fetching datum files from the web.");
        throw new RuntimeException(ex);
      }
    }
  }

  /**
   * Verify that the needed elevation data exists in the cache and if it does not exist, try to
   * download it. The graph is used to determine the extent of the NED.
   */
  @Override
  public void fetchData(Graph graph) {
    tileSource.fetchData(graph, cacheDir);
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
   * because the Earth’s mass is irregularly distributed. Ellipsoid datums like NAD83 are smooth
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
  private void loadVerticalDatum() {
    if (datums == null) {
      datums = new ArrayList<>();
      try {
        for (String filename : DATUM_FILENAMES) {
          VerticalDatum datum = VerticalDatum.fromGTX(cacheDir.entry(filename).asInputStream());
          datums.add(datum);
        }
      } catch (IOException e) {
        LOG.error("Datum file has disappeared since preflight inputs check.");
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Grab the rather voluminous vertical datum files from the OTP web server and save them in the
   * NED cache directory.
   */
  private void fetchDatum() throws Exception {
    LOG.info("Attempting to fetch datum files from OTP project web server...");
    ZipInputStream zis = new ZipInputStream(datumUrl.openStream());
    for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
      String filename = entry.getName();
      if (entry.isDirectory() || filename.contains("/") || filename.contains("\\")) {
        throw new RuntimeException("ZIP files containing directories are not supported");
      }
      LOG.info("decompressing {}", filename);
      OutputStream os = cacheDir.entry(filename).asOutputStream();
      ByteStreams.copy(zis, os);
      os.close();
    }
    zis.close();
    LOG.info("Done.");
  }
}
