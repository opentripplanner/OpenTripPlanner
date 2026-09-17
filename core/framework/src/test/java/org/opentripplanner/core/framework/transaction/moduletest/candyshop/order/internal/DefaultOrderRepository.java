package org.opentripplanner.framework.transaction.moduletest.candyshop.order.internal;

import java.util.Collection;
import org.opentripplanner.framework.transaction.moduletest.candyshop.base.EntityMap;
import org.opentripplanner.framework.transaction.moduletest.candyshop.order.OrderRepository;
import org.opentripplanner.framework.transaction.moduletest.candyshop.order.OrderService;
import org.opentripplanner.framework.transaction.moduletest.candyshop.order.model.Order;

/**
 * The repository implementation implements both the {@link OrderRepository} and
 * {@link OrderService}. The {@link OrderService} also play the role of the snapshot.
 */
public class DefaultOrderRepository implements OrderRepository, OrderService {

  private final EntityMap<Order> orders;

  DefaultOrderRepository(EntityMap<Order> orders) {
    this.orders = orders;
  }

  public static OrderRepository of() {
    return new DefaultOrderRepository(EntityMap.of());
  }

  public Order save(Order order) {
    return orders.save(order);
  }

  public Collection<Integer> listIds() {
    return orders.listIds();
  }

  public OrderService freeze() {
    return new DefaultOrderRepository(orders.copyOf());
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + "(" + System.identityHashCode(this) + ")";
  }
}
