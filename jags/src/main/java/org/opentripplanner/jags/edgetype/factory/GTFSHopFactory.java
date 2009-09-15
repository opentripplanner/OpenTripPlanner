package org.opentripplanner.jags.edgetype.factory;

import java.util.ArrayList;
import java.util.Collection;

import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.gtfs.Feed;
import org.opentripplanner.jags.gtfs.StopTime;
import org.opentripplanner.jags.gtfs.Trip;



public class GTFSHopFactory {
	private Feed feed;
	
	public GTFSHopFactory(Feed feed) throws Exception {
		this.feed = feed;
	}
	
	public ArrayList<Hop> run(boolean verbose) throws Exception {
		if(verbose){System.out.println("\tLoading stoptimes");}
		this.feed.loadStopTimes();
		if(verbose){System.out.println("\tLoading calendar dates");}
		this.feed.loadCalendarDates();
		
		ArrayList<Hop> ret = new ArrayList<Hop>();
		
		//Load hops
		Collection<Trip> trips = feed.getAllTrips();
		int j=0;
		int n=trips.size();
		for(Trip trip : trips ) {
			j++;
			if(verbose && j%(n/100)==0){ System.out.println( "Trip "+j+"/"+n ); }
			ArrayList<StopTime> stoptimes = trip.getStopTimes();
			for(int i=0; i<stoptimes.size()-1; i++) {
				StopTime st0 = stoptimes.get(i);
				StopTime st1 = stoptimes.get(i+1);
				Hop hop = new Hop( st0, st1 );
				ret.add( hop );
			}
		}
		
		return ret;
	}
	
	public ArrayList<Hop> run() throws Exception {
		return run(false);
	}
}
