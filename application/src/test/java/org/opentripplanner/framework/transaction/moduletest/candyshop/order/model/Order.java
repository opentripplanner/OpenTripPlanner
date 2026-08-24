package org.opentripplanner.framework.transaction.moduletest.candyshop.order.model;

import org.opentripplanner.framework.transaction.moduletest.candyshop.base.Entity;

public record Order(Integer id, String description) implements Entity {}
