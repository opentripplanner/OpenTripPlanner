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

package org.opentripplanner.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Primitives;

/**
 * This class constructs new instances of another class, then fills in the new instance's fields using
 * key-value pairs of Strings. For the moment these may come from query parameters or Jackson JSON nodes,
 * but in principle they could come from anywhere.
 *
 * The intended use is for objects representing incoming requests that have large numbers of parameters. Rather than
 * referencing every field by name when setting up defaults, then referencing them all again when handling each request
 * (possibly even multiple times in different HTTP method handlers), we assume that all query parameters and JSON config
 * fields will have exactly the same names as the Java object fields. This is getting uncomfortably close to Spring
 * configuration, and we should be careful to only use it in places where it truly makes code more readable.
 *
 * TODO we should probably also use this for RoutingResource to make the system uniform between JSON and QParams.
 * TODO make this stateless (a static method) if it's not too slow
 *
 * An instance of the requested class is first instantiated via its 0-argument constructor. Any initialization and
 * defaults should be handled at this point (in the constructor or in field initializer expressions).
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
 * @author abyrd
 */
public class ReflectiveInitializer<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectiveInitializer.class);
    protected final Class<T> targetClass;
    private final Map<String, Target> targets = Maps.newHashMap();
    
    public ReflectiveInitializer(Class<T> targetClass) {
        this.targetClass = targetClass;
        for (Field field : targetClass.getFields()) {
            Target target = FieldTarget.instanceFor(field);
            if (target != null) targets.put(target.name, target);
        }
        /* Scan methods after fields, so they will override fields with the same name. */
        for (Method method : targetClass.getMethods()) {
            Target target = MethodTarget.instanceFor(method);
            if (target != null) targets.put(target.name, target);
        }
        LOG.debug("Created a query scraper for: {}", targetClass.getSimpleName());
        for (Target t : targets.values()) {
            LOG.debug("-- {}", t);
        }
    }

    /** Create a new instance of T, and set its field from the given key-value pairs. */
    public T scrape (Map<String, String> pairs) {
        T obj = null;
        try {
            obj = targetClass.newInstance();
            // TODO iterate over incoming kv pairs rather than targets so we can warn when some don't match.
            for (Target t : targets.values()) {
                t.apply(pairs, obj);
            }
        } catch (Exception ex) {
            LOG.warn("exception {} while scraping {}", ex, targetClass);
        }
        return obj;
    }

    /** This converts everything to Strings and back, but it does work, and avoids a bunch of type conditionals. */
    public T scrape(JsonNode rootNode) {
        Map<String, String> pairs = Maps.newHashMap();
        // Ugh, there has to be a better way to do this.
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = rootNode.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldIterator.next();
            pairs.put(field.getKey(), field.getValue().asText());
        }
        return scrape(pairs);
    }

    private static abstract class Target {
        final String name;
        final Constructor<?> constructor;
        private Target (String name, Constructor<?> constructor) {
            this.name = name; // upper/lower case?
            this.constructor = constructor;
        }
        boolean apply(Map<String, String> pairs, Object obj) throws Exception {
            String value = pairs.get(name);
            if (value == null)
                return false;
            try {
                apply0(obj, constructor.newInstance(value));
                LOG.info("Initialized '{}' with value {}.", name, value);
                return true;
            } catch (Exception e) {
                LOG.warn("exception {} while applying {}", e, this);
                return false;
            }
        }
        abstract void apply0(Object obj, Object value) throws Exception;
    }

    private static class FieldTarget extends Target {
        final Field target;
        private FieldTarget(Field field, Constructor<?> cons) {
            super(field.getName(), cons);
            target = field;
        }
        static Target instanceFor(Field f) {
            Constructor<?> c = stringConstructor(f.getType());
            if (c == null) return null;
            return new FieldTarget(f, c);
        }
        @Override
        void apply0(Object obj, Object value) throws Exception { 
            target.set(obj, value); 
        }
        @Override
        public String toString () {
            return String.format("%s %s = %s('%s')", target.getType().getSimpleName(), 
                   target.getName(), constructor.getName(), name);
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
        public String toString () {
            return String.format("%s(%s('%s'))", target.getName(), constructor.getName(), name);
        }
    }   
    
    public static Constructor<?> stringConstructor(Class<?> clazz) {
        clazz = Primitives.wrap(clazz);
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length != 1) continue;
            if (params[0].equals(String.class)) return constructor;
        }
        return null;
    }

}
