package org.opentripplanner.analyst.cluster;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.PointSetCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * TODO what does this do? Does it really need to be a subclass?
 */
public class PointSetDatastore extends PointSetCache {

	static private File POINT_DIR = new File("cache", "pointsets");
	private String pointsetBucket;
	
	private AmazonS3Client s3;
	private final Boolean workOffline;
	
	public PointSetDatastore(Integer maxCacheSize, String s3CredentialsFilename,
			Boolean workOffline, String pointsetBucket){
		
		super();

		// allow the data store to work offline with cached data and skip S3 connection
		this.workOffline = workOffline;
		
		this.pointsetBucket = pointsetBucket;
		
		if(!this.workOffline) {
			if (s3CredentialsFilename != null) {
				AWSCredentials creds = new ProfileCredentialsProvider(s3CredentialsFilename, "default").getCredentials();
				s3 = new AmazonS3Client(creds);
			}
			else {
				// default credentials providers, e.g. IAM role
				s3 = new AmazonS3Client();
			}
		}
		
		// set up the cache
		this.pointSets = CacheBuilder.newBuilder()
			       .maximumSize(maxCacheSize)
			       .build(new S3PointSetLoader(workOffline, s3, pointsetBucket));
	}
	
	// adds file to S3 Data store or offline cache (if working offline)
	public String addPointSet(File pointSetFile, String pointSetId) throws IOException {
		if (pointSetId == null)
			throw new NullPointerException("null point set id");

		File renamedPointSetFile = new File(POINT_DIR, pointSetId + ".json");
		
		if (renamedPointSetFile.exists())
			return pointSetId;
		
		FileUtils.copyFile(pointSetFile, renamedPointSetFile);
		
		if(!this.workOffline) {
			// only upload if it doesn't exist
			try {
				s3.getObjectMetadata(pointsetBucket, pointSetId + ".json.gz");
			} catch (AmazonServiceException e) {
				// gzip compression in storage, not because we're worried about file size but to speed file transfer
				FileInputStream fis = new FileInputStream(pointSetFile);
				File tempFile = File.createTempFile(pointSetId, ".json.gz");
				FileOutputStream fos = new FileOutputStream(tempFile);
				GZIPOutputStream gos = new GZIPOutputStream(fos);
				
				try {
					ByteStreams.copy(fis, gos);
				} finally {
					gos.close();
					fis.close();
				}
				
				s3.putObject(pointsetBucket, pointSetId + ".json.gz", tempFile);
				tempFile.delete();
			}
		} 
		
		return pointSetId;

	}
	
	/** does this pointset exist in local cache? */
	public boolean isCached (String pointsetId) {
		return new File(POINT_DIR, pointsetId + ".json").exists();
	}
	
	/**
	 * Load pointsets from S3.
	 */
	protected static class S3PointSetLoader extends CacheLoader<String, PointSet> {

		private Boolean workOffline;
		private AmazonS3Client s3;
		private String pointsetBucket;
		
		/**
		 * Construct a new point set loader. S3 clients are generally threadsafe, so it's fine to share them.
		 */
		public S3PointSetLoader(Boolean workOffline, AmazonS3Client s3, String pointsetBucket) {
			this.workOffline = workOffline;
			this.s3 = s3;
			this.pointsetBucket = pointsetBucket;
		}

		@Override
		public PointSet load (String pointSetId) throws Exception {
			
			File cachedFile;
			
			if(!workOffline) {
				// get pointset metadata from S3
				cachedFile = new File(POINT_DIR, pointSetId + ".json");
				if(!cachedFile.exists()){
					POINT_DIR.mkdirs();
					
					S3Object obj = s3.getObject(pointsetBucket, pointSetId + ".json.gz");
					ObjectMetadata objMet = obj.getObjectMetadata();
					FileOutputStream fos = new FileOutputStream(cachedFile);
					GZIPInputStream gis = new GZIPInputStream(obj.getObjectContent());
					try {
						ByteStreams.copy(gis, fos);
					} finally {
						fos.close();
						gis.close();
					}
				}
			}
			else 
				cachedFile = new File(POINT_DIR, pointSetId + ".json");
			
			
			
			// grab it from the cache
			
			return PointSet.fromGeoJson(cachedFile);	
		}
	}

	@Override
	public List<String> getPointSetIds() {
		// we have no clue what is in the S3 bucket.
		throw new UnsupportedOperationException("S3-backed point set datastore does not know what pointsets are available.");
	}
}
