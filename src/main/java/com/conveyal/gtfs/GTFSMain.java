package com.conveyal.gtfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GTFSMain {

    private static final Logger LOG = LoggerFactory.getLogger(GTFSMain.class);

    static String INPUT = "/var/otp/graphs/dc/wmata.zip";
    //static final String INPUT = "/var/otp/graphs/nl/gtfs-nl.zip";
    //static final String INPUT = "/var/otp/graphs/trimet/gtfs.zip";
    
    public static void main (String[] args) {
        if (args.length >= 1)
            INPUT = args[0];
            
        GTFSFeed feed = GTFSFeed.fromFile(INPUT);
        feed.findPatterns();
        
        if (args.length == 2)
            feed.toFile(args[1]);
        
        feed.db.close();
    }

}
