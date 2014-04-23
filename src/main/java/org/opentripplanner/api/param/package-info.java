/**
 * This package contains classes modeling parameters passed into web services in the query string.
 * These always begin as strings and must be parsed into an internal representation.
 *
 * Jersey will inject @QueryParam-annotated fields and method parameters which can be constructed
 * from a single String. The QueryParameter abstract base class has this characteristic, and a new
 * concrete parameter class can be created by extending this base class and implementing the parse
 * method.
 */
package org.opentripplanner.api.param;