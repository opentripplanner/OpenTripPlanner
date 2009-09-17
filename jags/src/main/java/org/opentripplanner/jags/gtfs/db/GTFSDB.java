package org.opentripplanner.jags.gtfs.db;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

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
	
	public void store(PackagedFeed feed) throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {

		Session session = sessionFactory.getCurrentSession();
		session.beginTransaction();
		
		for( Stop stop : feed.stopTable) {
			System.out.println( stop );
			session.save(stop);
		}
		
		session.getTransaction().commit();
	}
	
	public List<Stop> get() {
        Session session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        List<Stop> result = session.createQuery("from Stop").list();
        session.getTransaction().commit();
        return result;
	}
	
	public static void main(String[] args) throws Exception {
		PackagedFeed pfeed = new PackagedFeed( "caltrain_gtfs.zip" );
		
		GTFSDB gtfsdb = new GTFSDB();
		gtfsdb.store(pfeed);
		
		List<Stop> stops = gtfsdb.get();
		System.out.println( stops );
	}
}
