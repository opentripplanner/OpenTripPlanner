package org.opentripplanner.api.parameter;

import java.util.List;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;

/**
 * An ordered list of sets of qualified modes. For example, if someone was in possession of a car
 * and wanted to park it and/or walk before taking a train or a tram, and finally rent a bicycle to
 * reach the destination: CAR_HAVE_PARK,WALK;TRAIN,TRAM;BIKE_RENT
 * It might also make sense to allow slashes meaning "or", or simply the word "or".
 */
public class QualifiedModeSetSequence {

    public List<Set<QualifiedMode>> qModeSets = Lists.newArrayList();
    
    public QualifiedModeSetSequence(String s) {
        for (String seg : s.split(";")) {
            Set<QualifiedMode> qModeSet = Sets.newHashSet();
            for (String qMode : seg.split(",")) {
                qModeSet.add(new QualifiedMode(qMode));
            }
            if (!qModeSet.isEmpty()) {
                qModeSets.add(qModeSet);
            }
                
        }
    }
}
