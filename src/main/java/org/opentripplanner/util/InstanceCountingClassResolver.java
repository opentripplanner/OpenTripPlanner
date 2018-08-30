package org.opentripplanner.util;

import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * This class is used for configuring and optimizing Kryo configuration.
 * Kryo lets you register specific serializers for specific concrete classes, but also has quite a few built-in default
 * serializers for individual classes or trees of classes under some interface (e.g. Collection).
 * Writing and registering a custom serializer or choosing between a third party class's built-in Externalizable method
 * and such a custom serializer, can have significant effects on the speed of serialization and size of the resulting
 * serialized data. In order to apply the 80/20 rule it's helpful to have a list of how often various classes are
 * serialized in your application.
 *
 * This class works by wrapping one method in the Kryo framework that is called for every serialized instance: the
 * method that looks up the serializer for a given class. It counts how many times each class is looked up, and can
 * print out a summary of those frequencies as well as the serializer that was associated with each class.
 *
 * Note that if you have already registered serializers that are non-recursive, e.g. they write out all the values
 * in an internal int array within an ArrayList implementation, those internal fields will of course not be counted.
 *
 * We could achieve the same thing by extending the ReferenceResolver in the same way, but here in ClassResolver we have
 * easy access to the map of Class-Serializer associations.
 *
 * To use this you need to specify a custom class resolver when creating Kryo:
 * Kryo kryo = new Kryo(new InstanceCountingClassResolver(), new MapReferenceResolver(), new DefaultStreamFactory());
 * kryo.setRegistrationRequired(false);
 * [...]
 * ((InstanceCountingClassResolver)kryo.getClassResolver()).summarize();
 *
 * Created by abyrd on 2018-08-29
 */
public class InstanceCountingClassResolver extends DefaultClassResolver {

    private TObjectIntMap<Class<?>> instanceCounts = new TObjectIntHashMap<>();

    public Registration getRegistration(Class type) {
        instanceCounts.adjustOrPutValue(type, 1, 1);
        return super.getRegistration(type);
    }

    public void summarize() {
        instanceCounts.forEachEntry((classe, count) -> {
            Registration registration = getRegistration(classe);
            String serializerName = registration.getSerializer().getClass().getSimpleName();
            System.out.println(count + " " + classe.getSimpleName() + " " + serializerName);
            return true;
        });
    }

}