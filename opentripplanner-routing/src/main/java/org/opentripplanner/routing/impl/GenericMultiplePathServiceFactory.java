/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.routing.services.PathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Factory keep a cache of previously created path services. If running low in memory, it will
 * remove oldest accessed graph. If the underlying graph file has been modified since last load, it
 * will be automatically reloaded.
 */
public abstract class GenericMultiplePathServiceFactory {

    private static final int MAX_RETRIES = 3;

    private static final int RETRY_INTERVAL = 3000;

    private static final Logger LOG = LoggerFactory
            .getLogger(GenericMultiplePathServiceFactory.class);

    private static class SubServicesBundle {

        public AbstractApplicationContext context;

        public long timestampLoaded;

        public PathService pathService;

        public PatchService patchService;

        public boolean reloadInProgress;
    }

    private String[] subApplicationContextList;

    private boolean asyncReload = false;

    private Map<String, SubServicesBundle> subServices = new HashMap<String, SubServicesBundle>();

    /**
     * @param subApplicationContext Spring application configuration to use for instantiating each
     *        service.
     */
    public void setSubApplicationContextList(String[] subApplicationContextList) {
        this.subApplicationContextList = subApplicationContextList;
    }

    /**
     * @param asyncReload Set async reload mode: If true, reload the new graph in the background
     *        while serving an old version to incoming requests. If false, block all incoming
     *        requests until new graph is reloaded. Please be aware that activating asyncReload will
     *        greatly increase memory requirements.
     */
    public void setAsyncReload(boolean asyncReload) {
        this.asyncReload = asyncReload;
    }

    /**
     * @param routerID
     * @param timestamp The timestamp to check against.
     * @return True if the data source has been modified since "timestamp".
     */
    protected abstract boolean checkReload(String routerID, long timestamp);

    /**
     * Register needed data source in the given application context.
     * 
     * @param context The application context to register the data source.
     * @param registry The bean registry.
     */
    protected abstract void registerDataSource(String routerID, ApplicationContext context,
            BeanDefinitionRegistry registry);

    protected PathService doGetPathService(String routerID) {
        return doGetSubServiceBundle(routerID).pathService;
    }

    protected PatchService doGetPatchService(String routerID) {
        return doGetSubServiceBundle(routerID).patchService;
    }

    private SubServicesBundle doGetSubServiceBundle(String routerID) {
        /* Ensure we have a PathServiceBundle, even an empty one, to synchronize on. */
        SubServicesBundle ssb = null;
        synchronized (subServices) {
            ssb = subServices.get(routerID);
            if (ssb == null) {
                ssb = new SubServicesBundle();
                ssb.pathService = null;
                ssb.timestampLoaded = System.currentTimeMillis();
                ssb.reloadInProgress = false;
                subServices.put(routerID, ssb);
            }
        }
        /*
         * Synchronize access to the bundle only, to prevent blocking requests to other graphs.
         * Check for first-time loading, no background reload is possible since no older version is
         * available.
         */
        synchronized (ssb) {
            if (ssb.pathService == null) {
                SubServicesBundle newSsb = loadSubServices(routerID);
                ssb.pathService = newSsb.pathService;
                ssb.patchService = newSsb.patchService;
                ssb.context = newSsb.context;
                ssb.timestampLoaded = System.currentTimeMillis();
                return ssb;
            }
        }
        /* Here background reload becomes possible. */
        boolean reload = false;
        synchronized (ssb) {
            if (checkReload(routerID, ssb.timestampLoaded) && !ssb.reloadInProgress) {
                if (!asyncReload) {
                    LOG.info("Reloading modified graph '" + routerID + "'");
                    // Sync reload: remove old version before loading new one, in synchronized
                    // block.
                    ssb.pathService = null;
                    ssb.patchService = null;
                    ssb.context.close();
                    ssb.context = null;
                    SubServicesBundle newSsb = loadSubServices(routerID);
                    ssb.pathService = newSsb.pathService;
                    ssb.patchService = newSsb.patchService;
                    ssb.context = newSsb.context;
                    ssb.timestampLoaded = System.currentTimeMillis();
                } else {
                    reload = true;
                    ssb.reloadInProgress = true;
                }
            }
        }
        if (reload) {
            // Async reload: load new version but keep old one while not ready for other
            // requests.
            SubServicesBundle newSsb = loadSubServices(routerID);
            synchronized (ssb) {
                LOG.info("Async reloading modified graph '" + routerID + "'");
                ssb.pathService = newSsb.pathService;
                ssb.patchService = newSsb.patchService;
                ssb.context.close();
                ssb.context = newSsb.context;
                ssb.reloadInProgress = false;
                ssb.timestampLoaded = System.currentTimeMillis();
            }
        }
        return ssb;
    }

    /**
     * Construct and load a new SubServicesBundle. Use default Spring application configuration to
     * build a new set of services.
     * 
     * @param routerID
     * @return A new PathService instance from a new ApplicationContext.
     */
    private SubServicesBundle loadSubServices(String routerID) {
        /*
         * Create a parent context containing the bundle.
         */
        AbstractApplicationContext parentContext = new StaticApplicationContext();
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) parentContext;
        registerDataSource(routerID, parentContext, registry);
        parentContext.refresh();

        int retries = 0;
        SubServicesBundle retval = new SubServicesBundle();
        while (true) {
            try {
                /*
                 * Create a new context to create a new path service, with all dependents services,
                 * using the default application context definition. The creation of a new context
                 * allow us to create new instances of service beans.
                 */
                retval.context = new ClassPathXmlApplicationContext(subApplicationContextList,
                        parentContext);
                AutowireCapableBeanFactory factory = retval.context.getAutowireCapableBeanFactory();
                retval.pathService = (PathService) factory
                        .getBean("pathService", PathService.class);
                retval.patchService = (PatchService) factory.getBean("patchService",
                        PatchService.class);
                break;

            } catch (BeanCreationException e) {
                /*
                 * Copying a new graph should use an atomic copy, but for convenience if it is not,
                 * we retry for a few times in case of truncated data before bailing out.
                 * 
                 * The original StreamCorruptedException is buried within dozen of layers of other
                 * exceptions, so we have to dig a bit (is this considered a hack?).
                 */
                boolean streamCorrupted = false;
                Throwable t = e.getCause();
                while (t != null) {
                    if (t instanceof StreamCorruptedException) {
                        streamCorrupted = true;
                        break;
                    }
                    t = t.getCause();
                }
                if (!streamCorrupted || retries++ > MAX_RETRIES)
                    throw e;
                LOG.warn("Can't load " + routerID + " (" + e + "): retrying...");
                try {
                    Thread.sleep(RETRY_INTERVAL);
                } catch (InterruptedException e1) {
                }
            }
        }
        return retval;
    }
}
