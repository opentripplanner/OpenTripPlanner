package org.opentripplanner.jags.gtfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;

import au.com.bytecode.opencsv.CSVReader;

public class Table implements Enumeration<String[]>{
	TableHeader header;
	CSVReader reader;
	String[] nextElement;
	Feed feed;
	
	Table( Feed feed, InputStream in ) throws IOException {
		this.feed = feed;
		reader = new CSVReader( new InputStreamReader( in ) );
		String[] columns = reader.readNext();
		header = new TableHeader(columns);
	}
	
	public TableHeader getHeader() {
		return header;
	}
	
	public boolean hasMoreElements() {
		try {
			nextElement = reader.readNext();
			return nextElement != null;
		} catch( IOException ex ) {
			return false;
		}
	}
	
	public String[] nextElement() {
		return nextElement;
	}
}
