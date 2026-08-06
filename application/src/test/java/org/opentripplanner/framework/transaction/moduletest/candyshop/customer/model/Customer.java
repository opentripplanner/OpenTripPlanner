package org.opentripplanner.framework.transaction.moduletest.candyshop.customer.model;

import org.opentripplanner.framework.transaction.moduletest.candyshop.base.Entity;

public record Customer(Integer id, String name) implements Entity {}
