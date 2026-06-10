package org.opentripplanner.graph_builder.module.ned;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.vertex.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Download one-degree-wide, 1/3 arcsecond NED tiles from S3 (or get them from a directory of files
 * organized as USGS organizes them when you ship them a hard drive).
 *
 * @author novalis
 */
public class DegreeGridNEDTileSource implements NEDTileSource {

  private static final Logger LOG = LoggerFactory.getLogger(DegreeGridNEDTileSource.class);

  private CompositeDataSource cacheDir;

  public String awsAccessKey;

  public String awsSecretKey;

  public String awsBucketName;

  private List<DataSource> nedTiles;

  @Override
  public void fetchData(Graph graph, CompositeDataSource cacheDir) {
    this.cacheDir = cacheDir;

    HashSet<IntCoordinate> tiles = new HashSet<>();

    for (Vertex v : graph.getVertices()) {
      Coordinate coord = v.getCoordinate();
      tiles.add(new IntCoordinate((int) coord.x, (int) coord.y));
    }

    List<DataSource> paths = new ArrayList<>();
    for (var tile : tiles) {
      int x = tile.x - 1;
      int y = tile.y + 1;
      DataSource tilePath = getOrDownloadTile(x, y);
      if (tilePath != null) {
        paths.add(tilePath);
      }
    }
    if (paths.isEmpty()) {
      throw new RuntimeException("No elevation tiles were able to be downloaded!");
    }
    nedTiles = paths;
  }

  @Override
  public List<DataSource> getNEDTiles() {
    return nedTiles;
  }

  private String formatLatLon(int x, int y) {
    String northSouth, eastWest;
    if (y < 0) {
      northSouth = "s";
      y = -y;
    } else {
      northSouth = "n";
    }
    if (x < 0) {
      eastWest = "w";
      x = -x;
    } else {
      eastWest = "e";
    }
    return String.format("%s%d%s%03d", northSouth, y, eastWest, x);
  }

  private DataSource getOrDownloadTile(int x, int y) {
    String key = formatLatLon(x, y) + ".tiff";
    DataSource tile = cacheDir.entry(key);
    if (tile.exists()) {
      return tile;
    }

    if (awsAccessKey == null || awsSecretKey == null) {
      throw new RuntimeException(
        "Cannot download NED tiles from S3: awsAccessKey or awsSecretKey properties are not set"
      );
    }
    LOG.info("Downloading NED degree tile {}", key);
    AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);
    try {
      S3Service s3Service = new RestS3Service(awsCredentials);
      S3Object object = s3Service.getObject(awsBucketName, key);

      InputStream istream = object.getDataInputStream();
      try (var ostream = tile.asOutputStream()) {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = istream.read(buffer)) != -1) {
          ostream.write(buffer, 0, read);
        }
      }
      istream.close();
    } catch (S3ServiceException e) {
      // Check if the error code is a NoSuchKey code which indicates that the file was not found in
      // the S3 bucket. If this is the cause, allow execution to continue without this tile.
      //
      // Note: The IAM policy for the provided credentials must allow both s3:GetObject and
      // s3:ListBucket for the target bucket. If just GetObject is provided, the S3ServiceException
      // will instead indicate a forbidden access error.
      if (e.getS3ErrorCode().equals("NoSuchKey")) {
        LOG.error("Elevation tile {} missing from s3bucket. Proceeding without tile!", key);
        return null;
      }
      throw new RuntimeException(e);
    } catch (ServiceException | IOException e) {
      throw new RuntimeException(e);
    }
    return tile;
  }

  private record IntCoordinate(int x, int y) {}
}
