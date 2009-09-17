package org.opentripplanner.jags.gtfs.db;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.opentripplanner.jags.gtfs.Feed;
import org.opentripplanner.jags.gtfs.PackagedFeed;
import org.opentripplanner.jags.gtfs.Stop;

public class GTFSDB {
	SessionFactory sessionFactory;
	
	public GTFSDB() {
		sessionFactory = new Configuration().configure().buildSessionFactory();
	}
	
	public void store(Feed feed) throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		feed.loadStops();
		
		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();
		
		for( Stop stop : feed.getAllStops()) {
			System.out.println( stop );
			session.save(stop);
		}
		
		session.getTransaction().commit();
	}
	
	public static void main(String[] args) throws Exception {
		PackagedFeed pfeed = new PackagedFeed( "caltrain_gtfs.zip" );
		Feed feed = new Feed(pfeed);
		
		GTFSDB gtfsdb = new GTFSDB();
		gtfsdb.store(feed);
	}
}
