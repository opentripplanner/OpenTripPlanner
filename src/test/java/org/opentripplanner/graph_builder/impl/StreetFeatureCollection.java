/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentripplanner.graph_builder.impl;

import java.util.List;

/**
 *
 * @author mabu
 */
public class StreetFeatureCollection {
    private List<StreetFeature> features;

    public List<StreetFeature> getFeatures() {
        return features;
    }

    public StreetFeatureCollection(List<StreetFeature> features) {
        this.features = features;
    }
    
}
