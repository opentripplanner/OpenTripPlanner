package org.opentripplanner.api.standalone;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

@Provider
public class JerseyInjector implements InjectableProvider<Resource, Type> {

    static Map<Type, Object> m = new HashMap<Type, Object>();
    
    public static void put(Type key, Object value) {
        m.put(key, value);
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.Singleton;
    }

    @Override
    public Injectable<Object> getInjectable(ComponentContext ic, Resource r, final Type t) {
        return new Injectable<Object>() {
            @Override
            public Object getValue() {
                return m.get(t);
            }
        };
    }

}
