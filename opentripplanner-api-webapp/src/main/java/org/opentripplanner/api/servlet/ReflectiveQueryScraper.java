package org.opentripplanner.api.servlet;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Servlet filter that constructs new objects of a given class, using reflection to pull their 
 * field values directly from the query parameters in an HttpRequest. It then seeds the request 
 * scope by storing a reference to the constructed object as an attribute of the HttpRequest itself.
 * 
 * An instance of the requested class is first instantiated via its 0-argument constructor. Any 
 * initialization and defaults should be handled at this point. 
 * 
 * Next, field and setter method names are matched with query parameters in the incoming 
 * HttpRequest. Fields whose declared type has a constructor taking a single String argument 
 * (including String itself) will be set from the query parameter having the same name, if one 
 * exists. Setter methods will also be considered if 1) they have a single argument, and 2) that
 * argument's class has a constructor with a single String argument.
 *  
 * Query parameters are matched with setter methods according to the usual convention: 
 * changing the first character to upper case and prepending 'set'. A setter method invocation will 
 * be preferred to directly setting the field with the corresponding name, if it exists.
 * 
 * @author andrewbyrd
 */
public class ReflectiveQueryScraper {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectiveQueryScraper.class);
    protected final Class<?> targetClass;
    private final Set<Target> targets;
    
    public ReflectiveQueryScraper(Class<?> targetClass) {
        this.targetClass = targetClass;
        targets = new HashSet<Target>();
        for (Field field : targetClass.getFields()) {
            Target target = FieldTarget.instanceFor(field);
            if (target != null)
                targets.add(target);
        }
        for (Method method : targetClass.getMethods()) {
            Target target = MethodTarget.instanceFor(method);
            if (target != null)
                targets.add(target);
        }
        LOG.info("initialized query scraper for: {}", targetClass);
        for (Target t : targets)
            LOG.info("-- {}", t);
    }

    public Object scrape(ServletRequest request) {
        Object obj = null;
        try {
            obj = targetClass.newInstance();
            for (Target t : targets)
                t.apply(request, obj);
        } catch (Exception e) {
            LOG.warn("exception {} while scraping {}", e, targetClass);
        }
        return obj;
    }

    private static abstract class Target {
        final String param;
        final Constructor<?> constructor;
        private Target (String param, Constructor<?> constructor) { 
            this.param = param; // upper/lower case?
            this.constructor = constructor;
        }
        // NOTE: hashCode and equals reference only the param name. Collisions are intentional.
        @Override public int hashCode() { 
            return param.hashCode(); 
        }
        @Override public boolean equals(Object other) { 
            return other instanceof Target && ((Target)other).param == this.param; 
        }
        boolean apply(ServletRequest req, Object obj) throws Exception {
            String value = req.getParameter(param);
            if (value == null)
                return false;
            try {
                apply0(obj, constructor.newInstance(value));
                return true;
            } catch (Exception e) {
                LOG.warn("exception {} while applying {}", e, this);
                return false;
            }
        }
        abstract void apply0(Object obj, Object value) throws Exception;
        abstract Member getTarget();
    }    

    private static class FieldTarget extends Target {
        final Field target;
        private FieldTarget(Field field, Constructor<?> cons) {
            super(field.getName(), cons);
            target = field;
        }
        static Target instanceFor(Field f) {
            Constructor<?> c = stringConstructor(f.getType());
            if (c == null)
                return null;
            return new FieldTarget(f, c);
        }
        @Override
        void apply0(Object obj, Object value) throws Exception { 
            target.set(obj, value); 
        }
        @Override 
        Member getTarget () { return target; } 
        @Override
        public String toString () {
            return String.format("%s %s = %s('%s')", target.getType().getSimpleName(), 
                   target.getName(), constructor.getName(), param);
        }
    }
    
    // TODO: setters match param names with query parameters (allowing multiple parameters);
    // setFoo disables direct setting of field 'foo'
    private static class MethodTarget extends Target {
        final Method target;
        private MethodTarget(String param, Method method, Constructor<?> cons) {
            super(param, cons);
            target = method;
        }
        static Target instanceFor(Method method) {
            String methodName = method.getName();
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1)
                return null;
            Constructor<?> c = stringConstructor(params[0]);
            if (c == null)
                return null;
            if ( ! methodName.startsWith("set"))
                return null;
            if (methodName.length() == 3)
                return null;
            String baseName = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
            return new MethodTarget(baseName, method, c);
        }
        @Override
        void apply0(Object obj, Object value) throws Exception {
            target.invoke(obj, value);
        }
        @Override 
        Member getTarget () { return target; } 
        @Override
        public String toString () {
            return String.format("%s(%s('%s'))", target.getName(), constructor.getName(), param);
        }
    }   
    
    public static Constructor<?> stringConstructor(Class<?> clazz) {
        clazz = filterPrimitives(clazz);
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length != 1)
                continue;
            if (params[0].equals(String.class))
                return constructor;
        }
        return null;
    }

    // Amazingly, there is really no better way of doing this. 
    // Guava does provide wrap/unwrap implementations in Primitives.
    public static Class<?> filterPrimitives(Class<?> clazz) {
        if (WRAPPERS.containsKey(clazz))
            return WRAPPERS.get(clazz);
        return clazz;
    }

    public static Map<Class<?>, Class<?>> WRAPPERS = new HashMap<Class<?>, Class<?>>();
    static {
        WRAPPERS.put(boolean.class, Boolean.class);
        WRAPPERS.put(byte.class,    Byte.class);
        WRAPPERS.put(double.class,  Double.class);
        WRAPPERS.put(float.class,   Float.class);
        WRAPPERS.put(int.class,     Integer.class);
        WRAPPERS.put(long.class,    Long.class);
        WRAPPERS.put(short.class,   Short.class);
    }
}
