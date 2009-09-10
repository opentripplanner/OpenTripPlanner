package org.opentripplanner.jags.gtfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;

import au.com.bytecode.opencsv.CSVReader;

public class Table implements Enumeration<String[]>{
	HashMap<String, Integer> header;
	CSVReader reader;
	String[] nextElement;
	Feed feed;
	
	Table( Feed feed, InputStream in ) throws IOException {
		this.feed = feed;
		reader = new CSVReader( new InputStreamReader( in ) );
		String[] rawheader = reader.readNext();
		header = new HashMap<String, Integer>();
		for(int i=0; i<rawheader.length; i++) {
			header.put(rawheader[i], new Integer(i));
		}
	}
	
	public HashMap<String, Integer> getHeader() {
		return header;
	}
	
	public int colIndex( String colname ) {
		Integer ix = header.get( colname );
		if( ix == null ) {
			return -1;
		} else {
			return ix.intValue();
		}
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
