package org.opentripplanner.common.diff;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import gnu.trove.map.TLongObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Perform a recursive deep comparison of objects.
 * This is intended for testing that graph building is reproducible and serialization restores an identical graph.
 * It should be kept relatively simple.
 *
 * This class is not threadsafe. It holds configuration and state internally. Each instance should therefore be used
 * for one comparison only and thrown away.
 */
public class ObjectDiffer {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectDiffer.class);

    /**
     * The maximum recursion depth during the comparison.
     * We want both depth limiting and cycle detection.
     */
    private static final int MAX_RECURSION_DEPTH = 300;

    /** Current depth of recursive diff. */
    private int depth = 0;

    /** Maximum recusrion depth reached so far in the comparison. */
    private int maxDepthReached = 0;

    /**
     * Objects that have already been compared in the left-hand (a) object. This avoids cycles and repeatedly comparing
     * the same object when reached through different reference chains.
     * This uses identity equality, because we only add types we expect to cause recursion, not value types where
     * each instance of e.g. an Integer could be a different object with the same value.
     */
    Set<Object> alreadySeen = Sets.newIdentityHashSet();

    protected Set<String> ignoreFields = Sets.newHashSet();

    protected Set<Class> ignoreClasses = Sets.newHashSet();

    protected Set<Class> useEquals = Sets.newHashSet();

    /** Accumulate all differences seen during the comparison. */
    List<Difference> differences = new ArrayList<>();

    private int nObjectsCompared = 0;

    private boolean compareIdenticalObjects = false;

    /**
     * Tell the Differ to execute the whole comparison procedure even when comparing two identical object references.
     * Normally such comparisons would be optimized away since they are by definition identical trees of objects.
     * However this is useful in tests of the comparison system itself - there are known to be no differences.
     */
    public void enableComparingIdenticalObjects() {
        compareIdenticalObjects = true;
    }

    /**
     * Field names to ignore on all objects.
     */
    public void ignoreFields(String... fields) {
        ignoreFields.addAll(Arrays.asList(fields));
    }

    /**
     * Classes of objects whose contents will not be examined.
     * We will still check that two classes of this same type exist in the object graph, but not compare their contents.
     * Currently does not include subclasses, an exact match of the concrete type is necessary.
     */
    public void ignoreClasses(Class... classes) {
        ignoreClasses.addAll(Arrays.asList(classes));
    }

    /**
     * Use equals to compare instances of these classes.
     * Currently does not include subclasses, an exact match of the concrete type is necessary.
     */
    public void useEquals(Class... classes) {
        useEquals.addAll(Arrays.asList(classes));
    }

    /**
     * This is the main entry point to compare two objects. It's also called recursively for many other
     * complex objects in the tree.
     */
    public void compareTwoObjects (Object a, Object b) {
        nObjectsCompared += 1;
        if (compareIdenticalObjects) {
            // Comparing objects to themselves anecdotally leads to much higher recursion depth (e.g. 80 instead of 40)
            // and a lot more work (double the number of object comparisons). In the case of a graph that has made a
            // round trip through serialization, these identical objects must be internal JVM objects.
            if (a == null && b == null) return;
        } else {
            // Checking that the two objects are identity-equal here is a simple optimization, skipping subtrees that
            // are known to be identical because they are rooted at the same object.
            // This also detects the case where both references are null.
            if (a == b) return;
        }
        if (a != null && b == null || a == null && b != null) {
            difference(a, b, "One reference was null but not the other.");
            return;
        }
        // We bail out before this type comparison when a or b is null, because a null reference has no type.
        // In most cases we know in the caller that the two fields being compared are in fact of the same type.
        Class<?> classToCompare = a.getClass();
        if (b.getClass() != classToCompare) {
            difference(a, b, "Classes are not the same: %s vs %s", classToCompare.getSimpleName(), b.getClass().getSimpleName());
            return;
        }
        // Skip comparison of instance contents by caller's request. We do this after checking that the two
        // objects are of the same class, so we just don't compare what's inside the Objects.
        if (ignoreClasses.contains(classToCompare)) {
            return;
        }
        if (isPrimitive(a) || useEquals.contains(a.getClass())) {
            if (!a.equals(b)) {
                difference(a, b,"Primitive %s value mismatch: %s vs %s", classToCompare.getSimpleName(), a.toString(), b.toString());
            }
            return;
        }
        // Object is non-primitive and non-trivial to compare. It is of a type susceptible to cause recursion.
        // Perform cycle detection.
        if (alreadySeen.contains(a)) {
            return;
        } else {
            alreadySeen.add(a); // FIXME for identityHashSet, this is adding duplicate Integers etc.
        }
        // We are going to recurse. Check that we won't exceed allowable depth.
        depth += 1;
        if (depth > MAX_RECURSION_DEPTH) {
            throw new RuntimeException("Max recursion depth exceeded: " + MAX_RECURSION_DEPTH);
        }
        if (depth > maxDepthReached) {
            maxDepthReached = depth;
        }
        // Choose comparison strategy for the objects based on their type.
        if (a instanceof Map) { // Use isAssignableFrom(classToCompare) ?
            compareMaps(new StandardMapWrapper((Map) a), new StandardMapWrapper((Map) b));
        }
        else if (a instanceof TLongObjectMap) {
            compareMaps(new TLongObjectMapWrapper((TLongObjectMap) a), new TLongObjectMapWrapper((TLongObjectMap) b));
        }
        else if (a instanceof Multimap) {
            compareMaps(new MultimapWrapper((Multimap) a), new MultimapWrapper((Multimap) b));
        }
        else if (a instanceof Collection) {
            // TODO maybe even generalize this to iterable?
            compareCollections((Collection) a, (Collection) b);
        }
        else if (classToCompare.isArray()){
            compareArrays(a, b);
        }
        else {
            compareFieldByField(a, b);
        }
        depth -= 1;
    }

    /**
     * The fallback comparison method - compare every field of two objects of the same class.
     * Note that the two objects passed in must already be known to be of the same class by the caller.
     */
    private void compareFieldByField (Object a, Object b) {
        Class classToCompare = a.getClass();
        List<Field> fieldsToCompare = getAllFields(classToCompare);
        for (Field field : fieldsToCompare) {
            try {
                field.setAccessible(true);
                Object valueA = field.get(a);
                Object valueB = field.get(b);
                compareTwoObjects(valueA, valueB);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Not "primitive" strictly speaking, but things that can be compared with equals and no recursion.
     */
    private boolean isPrimitive(Object value) {
        return value instanceof Number || value instanceof String || value instanceof Boolean || value instanceof Character;
    }

    /**
     * Map _keys_ must define hashcode and equals, and will not be compared beyond their own definition of these.
     * Note that arrays for example do not define deep equals, so will not be properly compared as map keys.
     */
    private void compareMaps(MapComparisonWrapper a, MapComparisonWrapper b) {
        for (Object aKey : a.allKeys()) {
            if (b.containsKey(aKey)) {
                Object aValue = a.get(aKey);
                Object bValue = b.get(aKey);
                compareTwoObjects(aValue, bValue);
            } else {
                difference(a, b, "Right map does not contain key %s", aKey.toString());
            }
        }
    }

    /**
     * This can also handle primitive arrays.
     */
    private void compareArrays (Object a, Object b) {
        if (Array.getLength(a) != Array.getLength(b)) {
            difference(a, b, "Array lengths do not match.");
            return;
        }
        for (int i = 0; i < Array.getLength(a); i++) {
            compareTwoObjects(Array.get(a, i), Array.get(b, i));
        }
    }

    private void compareCollections(Collection a, Collection b) {
        if (a.size() != b.size()) {
            difference(a, b,"Collections differ in size: %d vs %d",
                    a.size(), b.size());
            return;
        }
        if (a instanceof Set) {
            // FIXME this is a lousy implementation
            if (!a.equals(b)) {
                difference(a, b, "Sets are not equal.");
                return;
            }
        } else {
            // If it's not a set, assume collection is ordered, compare by parallel iteration.
            Iterator leftIterator = a.iterator();
            Iterator rightIterator = b.iterator();
            while (leftIterator.hasNext()) {
                compareTwoObjects(leftIterator.next(), rightIterator.next());
            }
        }

    }

   /**
     * Get all public and private fields of the given class and all its superclasses, except those fields we have
     * asked to ignore.
     * TODO allow ignoring fields by class with className.fieldName syntax
     */
    private List<Field> getAllFields (Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (! ignoreFields.contains(field.getName())) {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Record a difference and bail out if too many accumulate.
     */
    private void difference (Object a, Object b, String format, Object... args) {
        differences.add(new Difference(a, b).withMessage(format, args));
        if (differences.size() > 200) {
            this.printDifferences();
            throw new RuntimeException("Too many differences.");
            // TODO catch this exception in the entry point
        }
    }

    public void printDifferences () {
        if (differences.isEmpty()) {
            System.out.println("No differences were found.");
        }
        for (Difference difference : differences) {
            System.out.println(difference.message);
        }
        System.out.println("Number of objects compared was " + nObjectsCompared);
        System.out.println("Maximum recursion depth was " + maxDepthReached);
    }

    public boolean hasDifferences() {
        return differences.size() > 0;
    }
}
