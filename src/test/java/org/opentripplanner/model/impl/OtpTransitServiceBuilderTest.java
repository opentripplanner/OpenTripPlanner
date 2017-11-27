package org.opentripplanner.model.impl;

import org.junit.Test;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.IdentityBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Integer.valueOf;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.opentripplanner.model.impl.OtpTransitBuilder.generateNoneExistingIds;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (30.10.2017)
 */
public class OtpTransitServiceBuilderTest {

    private static final Integer ID_1 = valueOf(1);
    private static final Integer ID_4 = valueOf(4);
    private static final Integer ID_5 = valueOf(5);
    private static final Integer ID_6 = valueOf(6);

    @Test
    public void testGenerateNoneExistingIds() throws Exception {
        List<? extends IdentityBean<Integer>> list;

        // An empty list should not cause any trouble (Exception)
        generateNoneExistingIds(Collections.<FeedInfo>emptyList());


        // Generate id for one value
        list = singletonList(newEntity());
        generateNoneExistingIds(list);
        assertEquals(ID_1, list.get(0).getId());

        // Given two entities with no id and max Íd = 4
        list = Arrays.asList(
                newEntity(),
                newEntity(ID_4),
                newEntity()
        );
        // When
        generateNoneExistingIds(list);
        // Then expect
        // First new id to be: maxId + 1 = 4+1 = 5
        assertEquals(ID_5, id(list, 0));
        // Existing to still be 4
        assertEquals(ID_4, id(list, 1));
        // Next to be 6
        assertEquals(ID_6, id(list, 2));
    }


    /* private methods */

    private static Integer id(List<? extends IdentityBean<Integer>> list, int index) {
        return list.get(index).getId();
    }

    private static IdentityBean<Integer> newEntity() {
        return newEntity(null);
    }

    private static IdentityBean<Integer> newEntity(Integer id) {
        FeedInfo e = new FeedInfo();
        if(id != null) {
            e.setId(id);
        }
        return e;
    }
}