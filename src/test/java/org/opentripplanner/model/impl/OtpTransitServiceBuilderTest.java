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
import static org.opentripplanner.model.impl.OtpTransitServiceBuilder.generateNoneExistentIds;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (30.10.2017)
 */
public class OtpTransitServiceBuilderTest {

    private static final String ID_1 = "1";
    private static final String ID_4 = "4";
    private static final String ID_5 = "5";
    private static final String ID_6 = "6";

    @Test
    public void testGenerateNoneExistentIds() throws Exception {
        List<? extends IdentityBean<String>> list;

        // An empty list should not cause any trouble (Exception)
        generateNoneExistentIds(Collections.<FeedInfo>emptyList());


        // Generate id for one value
        list = singletonList(newEntity());
        generateNoneExistentIds(list);
        assertEquals(ID_1, list.get(0).getId());

        // Given two entities with no id and max Íd = 4
        list = Arrays.asList(
                newEntity(),
                newEntity(ID_4),
                newEntity()
        );
        // When
        generateNoneExistentIds(list);
        // Then expect
        // First new id to be: maxId + 1 = 4+1 = 5
        assertEquals(ID_5, id(list, 0));
        // Existing to still be 4
        assertEquals(ID_4, id(list, 1));
        // Next to be 6
        assertEquals(ID_6, id(list, 2));
    }


    /* private methods */

    private static String id(List<? extends IdentityBean<String>> list, int index) {
        return list.get(index).getId();
    }

    private static IdentityBean<String> newEntity() {
        return newEntity(null);
    }

    private static IdentityBean<String> newEntity(String id) {
        FeedInfo e = new FeedInfo();
        e.setId(id);
        return e;
    }
}