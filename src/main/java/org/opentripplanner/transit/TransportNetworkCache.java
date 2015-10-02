package org.opentripplanner.transit;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.standalone.CommandLineParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This holds one or more TransportNetworks keyed on unique strings.
 * This is a replacement for ClusterGraphBuilder.
 * TODO this should serialize any networks it builds, attempt to reload from disk, and copy serialized networks to S3.
 * Because (de)serialization is now about 2 orders of magnitude faster than building from scratch.
 */
public class TransportNetworkCache {

    private static final Logger LOG = LoggerFactory.getLogger(TransportNetworkCache.class);

    private AmazonS3Client s3 = new AmazonS3Client();

    private static final File CACHE_DIR = new File("cache", "graphs"); // reuse cached graphs from old analyst worker

    private final String sourceBucket;

    String currentNetworkId = null;

    TransportNetwork currentNetwork = null;

    public TransportNetworkCache(String sourceBucket) {
        this.sourceBucket = sourceBucket;
    }

    /**
     * Return the graph for the given unique identifier for graph builder inputs on S3.
     * If this is the same as the last graph built, just return the pre-built graph.
     * If not, build the graph from the inputs, fetching them from S3 to the local cache as needed.
     */
    public synchronized TransportNetwork getNetwork (String networkId) {

        LOG.info("Finding or building a TransportNetwork for ID {}", networkId);

        if (networkId.equals(currentNetworkId)) {
            LOG.info("Network ID has not changed. Reusing the last one that was built.");
            return currentNetwork;
        }

        TransportNetwork network = checkCached(networkId);
        if (network == null) {
            LOG.info("Cached transport network for id {} and commit {} was not found. Building it.",
                    networkId, MavenVersion.VERSION.commit);
            network = buildNetwork(networkId);
        }

        currentNetwork = network;
        currentNetworkId = networkId;

        return network;
    }

    /** If this transport network is already built and cached, fetch it quick */
    private TransportNetwork checkCached (String networkId) {
        try {
            String filename = networkId + "_" + MavenVersion.VERSION.commit + ".dat";
            File cacheLocation = new File(CACHE_DIR, networkId + "_" + MavenVersion.VERSION.commit + ".dat");

            if (cacheLocation.exists())
                // yippee! we have a cached network
                LOG.info("Found locally-cached transport network for id {} and commit {}", networkId, MavenVersion.VERSION.commit);
            else {
                LOG.info("Checking for cached transport network on S3");
                // try to download from S3
                S3Object tn;
                try {
                    tn = s3.getObject(sourceBucket, filename);
                } catch (AmazonServiceException ex) {
                    LOG.info("No cached transport network was found in S3");
                    return null;
                }

                CACHE_DIR.mkdirs();

                // get it on the disk to save it for later
                FileOutputStream fos = new FileOutputStream(cacheLocation);
                InputStream is = tn.getObjectContent();
                try {
                    ByteStreams.copy(is, fos);
                } finally {
                    is.close();
                    fos.close();
                }
            }

            FileInputStream fis = new FileInputStream(cacheLocation);
            try {
                return TransportNetwork.read(fis);
            } finally {
                fis.close();
            }
        } catch (Exception e) {
            LOG.error("Exception occurred retrieving cached transport network", e);
            return null;
        }
    }

    /** If we did not find a cached network, build one */
    public TransportNetwork buildNetwork (String networkId) {
        // The location of the inputs that will be used to build this graph
        File dataDirectory = new File(CACHE_DIR, networkId);

        // If we don't have a local copy of the inputs, fetch graph data as a ZIP from S3 and unzip it.
        if( ! dataDirectory.exists() || dataDirectory.list().length == 0) {
            LOG.info("Downloading graph input files from S3.");
            dataDirectory.mkdirs();
            S3Object graphDataZipObject = s3.getObject(sourceBucket, networkId + ".zip");
            ZipInputStream zis = new ZipInputStream(graphDataZipObject.getObjectContent());
            try {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File entryDestination = new File(dataDirectory, entry.getName());
                    // Are both these mkdirs calls necessary?
                    entryDestination.getParentFile().mkdirs();
                    if (entry.isDirectory())
                        entryDestination.mkdirs();
                    else {
                        OutputStream entryFileOut = new FileOutputStream(entryDestination);
                        IOUtils.copy(zis, entryFileOut);
                        entryFileOut.close();
                    }
                }
                zis.close();
            } catch (Exception e) {
                // TODO delete cache dir which is probably corrupted.
                LOG.info("Error retrieving transportation network input files", e);
            }
        } else {
            LOG.info("Input files were found locally. Using these files from the cache.");
        }

        // Now we have a local copy of these graph inputs. Make a graph out of them.
        CommandLineParameters params = new CommandLineParameters();
        TransportNetwork network = TransportNetwork.fromDirectory(new File(CACHE_DIR, networkId));

        // cache the network
        String filename = networkId + "_" + MavenVersion.VERSION.commit + ".dat";
        File cacheLocation = new File(CACHE_DIR, networkId + "_" + MavenVersion.VERSION.commit + ".dat");
        
        try {
            FileOutputStream fos = new FileOutputStream(cacheLocation);
            try {
                network.write(fos);
            } finally {
                fos.close();
            }

            // upload to S3
            s3.putObject(sourceBucket, filename, cacheLocation);
        } catch (Exception e) {
            // don't break here as we do have a network to return, we just couldn't cache it.
            LOG.error("Error saving cached network", e);
            cacheLocation.delete();
        }

        return network;
    }
}
