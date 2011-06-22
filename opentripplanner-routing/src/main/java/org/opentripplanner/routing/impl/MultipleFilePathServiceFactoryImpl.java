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

import java.io.File;

import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;

/**
 * This class create multiple PathService based on a pattern containing {}, where {} will be
 * replaced by the routerId.
 * 
 * Example: "/path/to/graphs/{}/Graph.obj"
 */
public class MultipleFilePathServiceFactoryImpl extends GenericMultiplePathServiceFactory implements
        PathServiceFactory {

    private String pathPattern;

    /**
     * @param pathPattern A pattern to locate a graph file based on the router ID. The {} special
     *        symbol will be replaced by the content of routerID.
     */
    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    @Override
    public PathService getPathService(String routerID) {
        return doGetPathService(routerID);
    }

    @Override
    public PatchService getPatchService(String routerID) {
        return doGetPatchService(routerID);
    }

    private File getPathFromRouterID(String routerID) {
        return new File(pathPattern.replace("{}", routerID));
    }

    @Override
    protected boolean checkReload(String routerID, long timestampLoaded) {
        File graphPath = getPathFromRouterID(routerID);
        GraphBundle graphBundle = new GraphBundle();
        graphBundle.setPath(graphPath);
        File filePath = graphBundle.getGraphPath();
        return (filePath.canRead() && filePath.lastModified() > timestampLoaded);
    }

    @Override
    protected void registerDataSource(String routerID, ApplicationContext context,
            BeanDefinitionRegistry registry) {
        File graphPath = getPathFromRouterID(routerID);
        BeanDefinition graphBundleBean = new RootBeanDefinition(GraphBundle.class);
        registry.registerBeanDefinition("graphBundle", graphBundleBean);
        GraphBundle graphBundle = (GraphBundle) context.getBean("graphBundle");
        graphBundle.setPath(graphPath);
        /*
         * Check if graph file is present to prevent trying to reload a non-existing file.
         */
        if (!graphBundle.getGraphPath().isFile() || !graphBundle.getGraphPath().canRead())
            throw new IllegalStateException("Trying to load non existing graph file "
                    + graphBundle.getGraphPath().getAbsolutePath());
    }
}
