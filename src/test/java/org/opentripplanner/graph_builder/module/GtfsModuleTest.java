package org.opentripplanner.graph_builder.module;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.opentripplanner.graph_builder.model.GtfsBundle;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GtfsModuleTest {

    @Test
    public void shouldSortByFileNameAlphabetically() {
        List<GtfsBundle> bundles = ImmutableList.of("C.gtfs", "c.gtfs", "/tmp/z.gtfs", "/x-files/001-a.gtfs", "/some/other/folder/b.gtfs")
                .stream()
                .map(name -> new GtfsBundle(new File(name))).collect(Collectors.toList());

        GtfsModule module = new GtfsModule(bundles);

        List<String> names = module.getGtfsBundles().stream().map(m -> m.getPath().getName()).collect(Collectors.toList());

        assertThat(names, is(ImmutableList.of("001-a.gtfs", "C.gtfs", "b.gtfs", "c.gtfs", "z.gtfs")));

    }
}