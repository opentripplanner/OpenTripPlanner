package org.opentripplanner.api.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Jersey uses java.util.logging for logging. It sends everything to stderr with a different
 * format than OTP internal logging calls. org.slf4j.bridge.SLF4JBridgeHandler redirects all JUL
 * calls to SLF4J API, which is the public interface of Logback.
 * If you tell SLF4JBridgeHandler to bridge inside the servlet, it doesn't affect Jersey logging. 
 * This is presumably because of how servlet containers isolate classloaders. By performing the
 * setup in a contextListener it actually applies to Jersey.
 * Messages from the servlet container may still go to stderr (this is the case for Tomcat). That
 * would probably need to be configured in the servlet container itself, if you care.
 * @author abyrd
 */
public class LoggingConfigurationListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent arg0) { }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
}
