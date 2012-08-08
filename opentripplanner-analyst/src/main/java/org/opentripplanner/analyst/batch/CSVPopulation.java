package org.opentripplanner.analyst.batch;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvReader;

import lombok.Setter;

public class CSVPopulation extends BasicPopulation {

    private static final Logger LOG = LoggerFactory.getLogger(BasicPopulation.class);

    @Setter
    public int latCol = 0;

    @Setter
    public int lonCol = 1;

    @Setter
    public int labelCol = 2;

    @Setter
    public int inputCol = 3;

    @Setter
    public boolean skipHeaders = true;

    @Override
    public void createIndividuals() {
        try {
            CsvReader reader = new CsvReader(sourceFilename, ',', Charset.forName("UTF8"));
            if (skipHeaders) {
                reader.readHeaders();
            }
            while (reader.readRecord()) {
                double lat = Double.parseDouble(reader.get(latCol));
                double lon = Double.parseDouble(reader.get(lonCol));
                String label = reader.get(labelCol);
                Double input = Double.parseDouble(reader.get(inputCol));
                Individual individual = individualFactory.build(label, lon, lat, input);
                this.addIndividual(individual);
                //LOG.debug(individual.toString());
            }
            reader.close();
        } catch (Exception e) {
            LOG.error("exception while loading individuals from CSV file:");
            e.printStackTrace();
        }
    }

}
