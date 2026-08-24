package org.opentripplanner.framework.transaction.moduletest.candyshop.order;

import org.opentripplanner.framework.transaction.moduletest.candyshop.base.EntityIdProvider;

/**
 * In this case we demonstrate that the service can also play the repository snapshot role.
 * So, in the application the service is injected, and at the same time it is used as the
 * repository snapshot in the transaction framework. This is maybe not the most intuitive
 * model, but illustrates the flexibility of the transaction framework.
 */
public interface OrderService extends EntityIdProvider {}
