package org.opentripplanner.core.framework.transaction.moduletest.candyshop.order.model;

import org.opentripplanner.core.framework.transaction.moduletest.candyshop.base.Entity;

public record Order(Integer id, String description) implements Entity {}
