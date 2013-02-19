package org.opentripplanner.graph_builder.impl;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 02/01/13
 * Time: 15:25
 * To change this template use File | Settings | File Templates.
 */
public class LoggerAppenderProvider {

    private static org.slf4j.Logger _log = LoggerFactory.getLogger(PruneFloatingIslands.class);

    public static String createCsvFile4LoggerCat(String fileName, String categoryName) {
        if (fileName.isEmpty()) return null;
        try{
            PatternLayout layout =  new PatternLayout("%m\n");
            FileAppender appender = new RollingFileAppender(layout, fileName, false);
            Logger.getLogger(categoryName).addAppender(appender);
        }catch (IOException ioe){
            _log.warn("could not create file appender for " + fileName + " duo to: " + ioe.getMessage());
            return null;
        }
        return categoryName;
    }

}
