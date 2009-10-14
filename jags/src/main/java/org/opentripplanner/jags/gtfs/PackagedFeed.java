package org.opentripplanner.jags.gtfs;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class PackagedFeed {
	
	private ZipFile zippedFeed;	
	public RouteTable routeTable;
	public StopTable stopTable;
	public StopTimeTable stopTimeTable;
	
	public PackagedFeed( String feed_name ) throws ZipException, IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
    	File ff = new File( feed_name );
		zippedFeed = new ZipFile(ff);
		
		stopTable = new StopTable( zippedFeed.getInputStream( zippedFeed.getEntry( "stops.txt" ) ) );
		stopTimeTable = new StopTimeTable( zippedFeed.getInputStream( zippedFeed.getEntry( "stop_times.txt" ) ) );
		routeTable = new RouteTable( zippedFeed.getInputStream( zippedFeed.getEntry( "routes.txt" ) ) );
	}

	public Table getTable( String tableName ) throws IOException {
		InputStream table_stream = zippedFeed.getInputStream( zippedFeed.getEntry( tableName+".txt" ) );
		return new Table( table_stream );
	}
	
	public ZipFile getZippedFeed() {
		return zippedFeed;
	}

}
