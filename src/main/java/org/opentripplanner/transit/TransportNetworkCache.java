package org.opentripplanner.transit;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.standalone.CommandLineParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
        currentNetwork = TransportNetwork.fromDirectory(new File(CACHE_DIR, networkId));
        currentNetworkId = networkId;
        currentNetwork.buildStopTrees();
        // TODO Save the built graph on S3 for other workers to use.
        return currentNetwork;

    }

}
