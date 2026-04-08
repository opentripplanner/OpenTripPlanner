package org.opentripplanner.street.model.edge;

/**
 * This is a marker interface which all extensions to the AStar routing must implement.
 * The context can be request specific and is passed into the router using
 * {@link org.opentripplanner.street.search.state.State}.
 * <p>
 * The extension, for example, a {@link StreetEdgeCostExtension} can obtain the context from
 * the state. Note! There is no way to provide a type safe API for this, an alternative would
 * be to use ThreadLocal, but then the thread state must be copied over in case of something is
 * executed in another thread. So, we go for a simple solution with the slight drawback that the
 * implementation must check it has the right context.
 * <p>
 * There are no methods on this interface, the type should be enough for any extention to
 * determin if the right context is fetched.
 */
public interface ExtensionRequestContext {}
