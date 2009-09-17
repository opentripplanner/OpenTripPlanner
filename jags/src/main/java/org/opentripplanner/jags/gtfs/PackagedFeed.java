package org.opentripplanner.jags.gtfs;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.opentripplanner.jags.gtfs.types.GTFSDate;

public class PackagedFeed {
	
	private ZipFile zippedFeed;	
	public StopTable stopTable;
	
	public PackagedFeed( String feed_name ) throws ZipException, IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
    	File ff = new File( feed_name );
		zippedFeed = new ZipFile(ff);
		
		stopTable = new StopTable( zippedFeed.getInputStream( zippedFeed.getEntry( "stops.txt" ) ) );
	}

	public Table getTable( String tableName ) throws IOException {
		InputStream table_stream = zippedFeed.getInputStream( zippedFeed.getEntry( tableName+".txt" ) );
		return new Table( table_stream );
	}
	
	public ZipFile getZippedFeed() {
		return zippedFeed;
	}

}
