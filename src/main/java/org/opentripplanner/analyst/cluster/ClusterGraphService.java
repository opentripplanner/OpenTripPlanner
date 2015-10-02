package org.opentripplanner.analyst.cluster;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.routing.services.GraphSource.Factory;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

// TODO does not really need to extend GraphService
public class ClusterGraphService extends GraphService { 

	static File GRAPH_DIR = new File("cache", "graphs");
	
	private String graphBucket;
	
	private Boolean workOffline = false;
	private AmazonS3Client s3;

	private static final Logger LOG = LoggerFactory.getLogger(GraphService.class);

	// don't use more than 60% of free memory to cache graphs
	private Map<String,Router> graphMap = Maps.newConcurrentMap();
	
	@Override
	public synchronized Router getRouter(String graphId) {
		
		GRAPH_DIR.mkdirs();
		
		if(!graphMap.containsKey(graphId)) {
			
			try {
				if (!bucketCached(graphId)) {
					if(!workOffline) {
						downloadGraphSourceFiles(graphId, GRAPH_DIR);
					}
				}
			} catch (IOException e) {
				LOG.error("exception finding graph {}", graphId, e);
			}
			
			CommandLineParameters params = new CommandLineParameters();
			params.build = new File(GRAPH_DIR, graphId);
			params.inMemory = true;
			GraphBuilder gbt = GraphBuilder.forDirectory(params, params.build);
			gbt.run();
			
			Graph g = gbt.getGraph();
			
			g.routerId = graphId;
			
			g.index(new DefaultStreetVertexIndexFactory());

			g.index.clusterStopsAsNeeded();
			
			Router r = new Router(graphId, g);
			
			// temporarily disable graph caching so we don't run out of RAM.
			// Long-term we will use an actual cache for this.
			//graphMap.put(graphId,r);
			
			return r;
					
		}
		else {
			return graphMap.get(graphId);
		}
	}

	public ClusterGraphService(String s3CredentialsFilename, Boolean workOffline, String bucket) {
		
		if(!workOffline) {
			if (s3CredentialsFilename != null) {
				AWSCredentials creds = new ProfileCredentialsProvider(s3CredentialsFilename, "default").getCredentials();
				s3 = new AmazonS3Client(creds);
			}
			else {
				// This will first check for credentials in environment variables or ~/.aws/credentials
				// then fall back on S3 credentials propagated to EC2 instances via IAM roles.
				s3 = new AmazonS3Client();
			}
			
			this.graphBucket = bucket;
		}
		
		this.workOffline = workOffline;
	}
	
	// adds either a zip file or graph directory to S3, or local cache for offline use
	public void addGraphFile(File graphFile) throws IOException {
		
		String graphId = graphFile.getName();
		
		if(graphId.endsWith(".zip"))
			graphId = graphId.substring(0, graphId.length() - 4);
		
		File graphDir = new File(GRAPH_DIR, graphId);
		
		if (graphDir.exists()) {
			if (graphDir.list().length == 0) {
				graphDir.delete();
			}
			else {
				return;
			}
		}
		
		// if we're here the directory has either been deleted or never existed
		graphDir.mkdirs();
		
		File graphDataZip = new File(GRAPH_DIR, graphId + ".zip");
				
		// if directory zip contents  store as zip
		// either way ensure there is an extracted copy in the local cache
		if(graphFile.isDirectory()) {
			FileUtils.copyDirectory(graphFile, graphDir);
			
			zipGraphDir(graphDir, graphDataZip);
		}
		else if(graphFile.getName().endsWith(".zip")) {
			FileUtils.copyFile(graphFile, graphDataZip);
			unpackGraphZip(graphDataZip, graphDir, false);
		}
		else {
			graphDataZip = null;
		}
			
		if(!workOffline && graphDataZip != null) {
			// only upload if it's not there already
			try {
				s3.getObject(graphBucket, graphId + ".zip");
			} catch (AmazonServiceException e) {
				s3.putObject(graphBucket, graphId+".zip", graphDataZip);
			}
		}
		
		graphDataZip.delete();
		
	}
	
	public synchronized File getZippedGraph(String graphId) throws IOException {
		
		File graphDataDir = new File(GRAPH_DIR, graphId);
		
		File graphZipFile = new File(GRAPH_DIR, graphId + ".zip");
		
		if(!graphDataDir.exists() && graphDataDir.isDirectory()) {
			
			FileOutputStream fileOutputStream = new FileOutputStream(graphZipFile);
			ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
			
			byte[] buffer = new byte[1024];
			
			for(File f : graphDataDir.listFiles()) {
				ZipEntry zipEntry = new ZipEntry(f.getName());
				zipOutputStream.putNextEntry(zipEntry);
	    		FileInputStream fileInput = new FileInputStream(f);

	    		int len;
	    		while ((len = fileInput.read(buffer)) > 0) {
	    			zipOutputStream.write(buffer, 0, len);
	    		}
	 
	    		fileInput.close();
	    		zipOutputStream.closeEntry();
			}
			
			zipOutputStream.close();
			
			return graphZipFile;
					
		}
		
		return null;
		
	}
	
	private static boolean bucketCached(String graphId) throws IOException {
		File graphData = new File(GRAPH_DIR, graphId);
		
		// check if cached but only as zip
		if(!graphData.exists()) {
			File graphDataZip = new File(GRAPH_DIR, graphId + ".zip");
			
			if(graphDataZip.exists()) {
				zipGraphDir(graphData, graphDataZip);
			}
		}
		
		
		return graphData.exists() && graphData.isDirectory();
	}

	private void downloadGraphSourceFiles(String graphId, File dir) throws IOException {

		File graphCacheDir = dir;
		if (!graphCacheDir.exists())
			graphCacheDir.mkdirs();

		File graphZipFile = new File(graphCacheDir, graphId + ".zip");

		File extractedGraphDir = new File(graphCacheDir, graphId);

		if (extractedGraphDir.exists()) {
			FileUtils.deleteDirectory(extractedGraphDir);
		}

		extractedGraphDir.mkdirs();

		S3Object graphZip = s3.getObject(graphBucket, graphId+".zip");

		InputStream zipFileIn = graphZip.getObjectContent();

		OutputStream zipFileOut = new FileOutputStream(graphZipFile);

		IOUtils.copy(zipFileIn, zipFileOut);
		IOUtils.closeQuietly(zipFileIn);
		IOUtils.closeQuietly(zipFileOut);

		unpackGraphZip(graphZipFile, extractedGraphDir);
	}

	private static void unpackGraphZip(File graphZipFile, File extractedGraphDir) throws ZipException, IOException {
		// delete by default
		unpackGraphZip(graphZipFile, extractedGraphDir, true);
	}
	
	private static void unpackGraphZip(File graphZipFile, File extractedGraphDir, boolean delete) throws ZipException, IOException {
		
		ZipFile zipFile = new ZipFile(graphZipFile);
		
		Enumeration<? extends ZipEntry> entries = zipFile.entries();

		while (entries.hasMoreElements()) {

			ZipEntry entry = entries.nextElement();
			File entryDestination = new File(extractedGraphDir, entry.getName());

			entryDestination.getParentFile().mkdirs();

			if (entry.isDirectory())
				entryDestination.mkdirs();
			else {
				InputStream entryFileIn = zipFile.getInputStream(entry);
				OutputStream entryFileOut = new FileOutputStream(entryDestination);
				IOUtils.copy(entryFileIn, entryFileOut);
				IOUtils.closeQuietly(entryFileIn);
				IOUtils.closeQuietly(entryFileOut);
			}
		}

		zipFile.close();

		if (delete) {
			graphZipFile.delete();
		}
	}
	
	private static void zipGraphDir(File graphDirectory, File zipGraphFile) throws IOException {
		
		FileOutputStream fileOutputStream = new FileOutputStream(zipGraphFile);
		ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
		
		byte[] buffer = new byte[1024];
		
		for(File f : graphDirectory.listFiles()) {
			if (f.isDirectory())
				continue;
			
			ZipEntry zipEntry = new ZipEntry(f.getName());
			zipOutputStream.putNextEntry(zipEntry);
    		FileInputStream fileInput = new FileInputStream(f);

    		int len;
    		while ((len = fileInput.read(buffer)) > 0) {
    			zipOutputStream.write(buffer, 0, len);
    		}
 
    		fileInput.close();
    		zipOutputStream.closeEntry();
		}
		
		zipOutputStream.close();
	}
	

	@Override
	public int evictAll() {
		graphMap.clear();
		return 0;
	}

	@Override
	public Collection<String> getRouterIds() {
		return graphMap.keySet();
	}

	@Override
	public Factory getGraphSourceFactory() {
		return null;
	}

	@Override
	public boolean registerGraph(String arg0, GraphSource arg1) {
		return false;
	}

	@Override
	public void setDefaultRouterId(String arg0) {	
	}
}
