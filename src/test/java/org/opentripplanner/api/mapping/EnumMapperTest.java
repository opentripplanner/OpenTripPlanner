package org.opentripplanner.api.mapping;

import org.junit.Test;
import org.opentripplanner.api.model.ApiAbsoluteDirection;
import org.opentripplanner.api.model.ApiRelativeDirection;
import org.opentripplanner.api.model.ApiVertexType;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.VertexType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class EnumMapperTest {
    private final static String MSG = "Assert that the API enums have the exact same values that "
            + "the domain enums of the same type, and that the specialized mapper is mapping all "
            + "values. If this assumtion does not hold, create a new test.";

    @Test
    public void map() {
        try {
            verifyExactMatch(
                    VertexType.class,
                    ApiVertexType.class,
                    VertexTypeMapper::mapVertexType
            );
            verifyExactMatch(
                    AbsoluteDirection.class,
                    ApiAbsoluteDirection.class,
                    AbsoluteDirectionMapper::mapAbsoluteDirection
            );
            verifyExactMatch(
                    RelativeDirection.class,
                    ApiRelativeDirection.class,
                    RelativeDirectionMapper::mapRelativeDirection
            );
        }
        catch (RuntimeException ex) {
            System.out.println(MSG);
            throw ex;
        }
    }

    private <D extends Enum<?>,A extends Enum<?>> void  verifyExactMatch(
            Class<D> domainClass, Class<A> apiClass, Function<D, A> mapper
    ) {
        List<A> rest = new ArrayList<A>(List.of(apiClass.getEnumConstants()));
        for (D it : domainClass.getEnumConstants()) {
            A result = mapper.apply(it);
            assertEquals("Map " + it, result.name(), it.name());
            rest.remove(result);
        }
        assertEquals("[]", rest.toString());
    }
}