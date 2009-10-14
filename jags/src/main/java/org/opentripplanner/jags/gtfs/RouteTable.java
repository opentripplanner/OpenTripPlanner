package org.opentripplanner.jags.gtfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import au.com.bytecode.opencsv.CSVReader;

public class RouteTable implements Iterable<Route> {

	TableHeader header;
	CSVReader reader;
		
	class TableIterator implements Iterator<Route> {
			
		Route nextElement;

		public boolean hasNext() {
			try {
				String[] record = reader.readNext();
				nextElement = new Route(header,record);
				return nextElement != null;
			} catch( Exception ex ) {
				return false;
			}
		}

		public Route next() {
			return nextElement;
		}

		public void remove() {}
			
	}
	
	RouteTable( InputStream in ) throws IOException {
		reader = new CSVReader( new InputStreamReader( in ) );
		String[] columns = reader.readNext();
		header = new TableHeader(columns);
	}
		
	public TableHeader getHeader() {
		return header;
	}

	public Iterator<Route> iterator() {
		return new TableIterator();
	}
}

