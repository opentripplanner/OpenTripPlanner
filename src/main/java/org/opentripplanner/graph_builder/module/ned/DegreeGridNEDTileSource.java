package org.opentripplanner.graph_builder.module.ned;

import org.locationtech.jts.geom.Coordinate;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.Graphwide;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Download one-degree-wide, 1/3 arcsecond NED tiles from S3 (or get them from a directory of files
 * organized as USGS organizes them when you ship them a hard drive).
 *
 * @author novalis
 *
 */
public class DegreeGridNEDTileSource implements NEDTileSource {
    private static Logger log = LoggerFactory.getLogger(DegreeGridNEDTileSource.class);

    private Graph graph;

    private File cacheDirectory;

    public String awsAccessKey;

    public String awsSecretKey;

    public String awsBucketName;

    private List<File> nedTiles;

    @Override
    public List<File> getNEDTiles() {
        return nedTiles;
    }

    @Override public void fetchData(Graph graph, File cacheDirectory) {
        this.graph = graph;
        this.cacheDirectory = cacheDirectory;

        HashSet<P2<Integer>> tiles = new HashSet<P2<Integer>>();

        for (Vertex v : graph.getVertices()) {
            Coordinate coord = v.getCoordinate();
            tiles.add(new P2<Integer>((int) coord.x, (int) coord.y));
        }

        List<File> paths = new ArrayList<File>();
        for (P2<Integer> tile : tiles) {
            int x = tile.first - 1;
            int y = tile.second + 1;
            File tilePath = getPathToTile(x, y);
            if (tilePath != null) {
                paths.add(tilePath);
            }
        }
        if (paths.size() == 0) {
            throw new RuntimeException("No elevation tiles were able to be downloaded!");
        }
        nedTiles = paths;
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

    private File getPathToTile(int x, int y) {
        File path = new File(cacheDirectory, formatLatLon(x, y) + ".tiff");
        if (path.exists()) {
            return path;
        } else {
            path.getParentFile().mkdirs();

            if (awsAccessKey == null || awsSecretKey == null) {
                throw new RuntimeException(
                    "Cannot download NED tiles from S3: awsAccessKey or awsSecretKey properties are not set");
            }
            log.info("Downloading NED degree tile " + path);
            // download the file from S3.
            AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);
            String key = formatLatLon(x, y) + ".tiff";
            try {
                S3Service s3Service = new RestS3Service(awsCredentials);
                S3Object object = s3Service.getObject(awsBucketName, key);

                InputStream istream = object.getDataInputStream();
                FileOutputStream ostream = new FileOutputStream(path);

                byte[] buffer = new byte[4096];
                while (true) {
                    int read = istream.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    ostream.write(buffer, 0, read);
                }
                ostream.close();
                istream.close();
            } catch (S3ServiceException e) {
                // Check if the error code is a NoSuchKey code which indicates that the file was not found in the S3
                // bucket. If this is the cause, allow execution to continue, but add an annotation about the missing
                // file.
                //
                // Note: The IAM policy for the provided credentials must allow both s3:GetObject and s3:ListBucket for
                // the target bucket. If just GetObject is provided, the S3ServiceException will instead indicate a
                // forbidden access error.
                if (e.getS3ErrorCode().equals("NoSuchKey")) {
                    log.error(
                        graph.addBuilderAnnotation(
                            new Graphwide(
                                String.format("Elevation tile %s missing from s3bucket. Proceeding without tile!", key)
                            )
                        )
                    );
                    return null;
                }
                // Some other error occurred.
                path.deleteOnExit();
                throw new RuntimeException(e);
            } catch (ServiceException | IOException e) {
                path.deleteOnExit();
                throw new RuntimeException(e);
            }
            return path;
        }

    }


}
