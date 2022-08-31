/**
 * Add support for Amazon S3 cloud storage, getting all input files and storing the graph.obj in the
 * cloud.
 * <p>
 * This implementation will use the existing {@link org.opentripplanner.standalone.config.ConfigLoader}
 * to load config from the local disk.
 */
package org.opentripplanner.ext.datastore.aws;
