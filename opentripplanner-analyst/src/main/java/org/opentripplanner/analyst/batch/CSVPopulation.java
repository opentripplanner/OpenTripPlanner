package org.opentripplanner.analyst.batch;

import java.io.FileReader;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Setter;

import au.com.bytecode.opencsv.CSVReader;

public class CSVPopulation extends BasicPopulation {

    private static final Logger LOG = LoggerFactory.getLogger(BasicPopulation.class);

	@Setter public int latCol = 0;
	@Setter public int lonCol = 1;
	@Setter public int labelCol = 2;
	@Setter public int inputCol = 3;
	@Setter public boolean headers = true;
	
	@PostConstruct
	public void loadIndividuals() {
		try {
			CSVReader reader = new CSVReader(new FileReader(sourceFilename));
		    String [] nextLine;
		    if (headers) {
		    	reader.readNext();
		    }
		    while ((nextLine = reader.readNext()) != null) {
		    	double lat = Double.parseDouble(nextLine[latCol]);
		    	double lon = Double.parseDouble(nextLine[lonCol]);
		    	String label = nextLine[labelCol];
		    	Double input = Double.parseDouble(nextLine[inputCol]);
		    	Individual individual = individualFactory.build(label, lon, lat, input);
		    	this.add(individual);
		    	//LOG.debug(individual.toString());
		    }
		    reader.close();
		} catch (Exception e) {
			LOG.error("exception while loading individuals from CSV file:");
			e.printStackTrace();
		}
	}
	
}
