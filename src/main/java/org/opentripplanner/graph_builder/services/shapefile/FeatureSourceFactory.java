package org.opentripplanner.graph_builder.services.shapefile;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;

public interface FeatureSourceFactory {
    public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource();

    public void cleanup();
    
    /** @see GraphBuilderModule#checkInputs()  */
    public void checkInputs();
}
