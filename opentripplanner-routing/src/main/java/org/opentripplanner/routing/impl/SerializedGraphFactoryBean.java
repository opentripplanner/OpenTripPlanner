package org.opentripplanner.routing.impl;

import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.core.Graph;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class SerializedGraphFactoryBean extends AbstractFactoryBean {

    private GraphBundle _graphBundle;

    public void setGraphBundle(GraphBundle graphBundle) {
        _graphBundle = graphBundle;
    }

    @Override
    public Class<?> getObjectType() {
        return Graph.class;
    }

    @Override
    protected Object createInstance() throws Exception {
        return GraphSerializationLibrary.readGraph(_graphBundle.getGraphPath());
    }

}
