package org.opentripplanner.standalone;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.core.spi.component.ioc.IoCFullyManagedComponentProvider;

/** 
 * Ultra-minimal implementation of a subset of JSR-330 dependency injection. 
 * It handles only singleton-scoped objects (not per-request or per-session lifecycles), and
 * these must be instantiated/constructed by the caller upon binding.
 * It handles only field injection on those objets, not constructor or method parameter 
 * injection. It does call @PostConstruct annotated methods when injection is complete.
 * It does not automatically work out the correct injection/initialization order. 
 * Injection and post-construct calls will be performed in the order that the bindings are 
 * added. It creates IoCComponentProviders for injection into Jersey REST resources.
 */
public class OTPComponentProviderFactory implements IoCComponentProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OTPComponentProviderFactory.class);
    private Map<Class<?>, Object> bindings = new HashMap<Class<?>, Object>();
    private List<Object> bindingOrder = new ArrayList<Object>(); 
    private boolean locked = false;

    /** Call the given class's 0-arg constructor and bind the resulting instance. */
    public void bind(Class<?> klass) {
        try {
            Constructor<?> ctor = klass.getDeclaredConstructor();
            ctor.setAccessible(true);
            bind(klass, ctor.newInstance());
        } catch (Exception e) {
            LOG.error("Unable to invoke 0-arg constructor on {}", klass);
            e.printStackTrace();
        }
    }

    public void bind(Class<?> key, Object value) {
        if (locked) {
            LOG.error("Attempt to add a binding to a completed set of bindings.");
        } else {
            if (key.isInstance(value)) {
                bindings.put(key, value);
                bindingOrder.add(value);
            } else {
                LOG.error("Type mismatch in binding: " + key + " " + value);
            }
        }
    }

    @Override
    public IoCComponentProvider getComponentProvider(Class<?> c) {
        if ( ! bindings.containsKey(c)) {
            LOG.debug("Requested component provider for unbound " + c);
            return null; // let Jersey internal provider take care of it
        }
        LOG.debug("Returning component provider for " + c);
        return new JerseyComponentProvider(bindings.get(c));
    }

    /** Ignore the ComponentContext, wrap the 1-arg version. */
    @Override
    public IoCComponentProvider getComponentProvider(ComponentContext cc, Class<?> c) {
        return getComponentProvider(c);
    }
    
    /** Call this method after all bindings have been established. */
    public void doneBinding() {
        locked = true;
        injectFields();
        invokePostConstructMethods();
    }

    /** Perform field injection on all bound instances, in the order they were bound. */
    private void injectFields() {
        LOG.info("Performing field injection on all bound instances."); 
        for (Object instance : bindingOrder) {
            LOG.debug("Performing field injection on class {}", instance.getClass());
            for (Field field : instance.getClass().getDeclaredFields()) {
                LOG.debug("Considering field {} for injection", field.getName());
                if (field.isAnnotationPresent(Inject.class) ||
                    field.isAnnotationPresent(Autowired.class)) { // since we're still using Spring
                    Object obj = bindings.get(field.getType());
                    LOG.debug("Injecting field {} on instance of {}", 
                              field.getName(), instance.getClass());
                    if (obj != null) {
                        try {
                            field.setAccessible(true);
                            field.set(instance, obj);
                        } catch (Exception ex) {
                            LOG.error("Failed to perform field injection: {}", ex.toString());
                            ex.printStackTrace();
                        }
                    } else {
                        LOG.error("Found no binding for {}", field.getType());
                    }                        
                }
            }
        }
    }

    /** Call all @PostConstruct-annotated methods on bound instances, in the order they were bound. */
    private void invokePostConstructMethods() {
        // do not look at the binding class (key), but the concrete type of the instance (value)
        for (Object instance : bindingOrder) {
            // iterate over all declared methods, not just the public ones
            for (Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    try {
                        LOG.info("Calling post-construct {}", method);
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (Exception ex) {
                        LOG.error("Failed to invoke post-construct: {}", ex.toString());
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /** Instances of this class are handed off to Jersey. */
    public static class JerseyComponentProvider implements IoCFullyManagedComponentProvider {

        final Object o;
        
        public JerseyComponentProvider(Object o) {
            this.o = o;
        }
        
        @Override
        public ComponentScope getScope() {
            return ComponentScope.Singleton; // we only bind objects in singleton scope
        }
        
        @Override
        public Object getInstance() {
            return o;
        }
    
    }
    
}
