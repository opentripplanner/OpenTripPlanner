package org.opentripplanner.graph_builder.impl.ned;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.routing.core.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;

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

    private String awsAccessKey;

    private String awsSecretKey;

    @Override
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public List<File> getNEDTiles() {

        Envelope extent = graph.getExtent();
        List<File> paths = new ArrayList<File>();
        for (int y = (int) extent.getMinY() + 1; y <= (int) extent.getMaxY() + 1; ++y) {
            for (int x = (int) extent.getMinX() - 1; x <= (int) extent.getMaxX() - 1; ++x) {
                paths.add(getPathToTile(x, y));
            }
        }
        return paths;
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
        return String.format("%s%d%s%d", northSouth, y, eastWest, x);
    }

    private File getPathToTile(int x, int y) {
        File path = new File(cacheDirectory, formatLatLon(x, y) + ".tiff");
        if (path.exists()) {
            return path;
        } else {
            path.getParentFile().mkdirs();

            if (awsAccessKey == null || awsSecretKey == null) {
                throw new RuntimeException("Cannot download NED tiles from S3: awsAccessKey or awsSecretKey properties are not set");
            }
            log.debug("Downloading NED degree tile " + path);
            // download the file from S3.
            AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);
            try {
                S3Service s3Service = new RestS3Service(awsCredentials);
                String key = formatLatLon(x, y) + ".tiff";
                S3Object object = s3Service.getObject("ned13", key);

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
                throw new RuntimeException(e);
            } catch (ServiceException e) {
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return path;
        }

    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

}
