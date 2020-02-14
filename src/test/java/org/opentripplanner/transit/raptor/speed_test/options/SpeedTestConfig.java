package org.opentripplanner.transit.raptor.speed_test.options;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class SpeedTestConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SpeedTestConfig.class);
    public static final String FILE_NAME = "speed-test-config.json";

    private LocalDate testDate = LocalDate.now();

    public void setTestDate(String testDate) {
        this.testDate = LocalDate.parse(testDate);
    }

    /**
     * The test date is the date used for all test cases. The default value is today.
     */
    public LocalDate getTestDate() {
        return testDate;
    }

    public static SpeedTestConfig config(File dir) {
        try {
            File configFile = new File(dir, FILE_NAME);

            if(!configFile.exists()) {
                LOG.warn(
                        "SpeedTest config file not found. Default " +
                        "config is used. Missing file: {}",
                        configFile.getAbsolutePath()
                );
                return new SpeedTestConfig();
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            return mapper.readValue(configFile, SpeedTestConfig.class);
        }
        catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
