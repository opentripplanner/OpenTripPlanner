package org.opentripplanner.utils.collection;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class ParentOrderTest {

  private record Item(String id, @Nullable String parent) {}

  private static List<String> order(Item... items) {
    return ParentOrder.parentsFirst(List.of(items), Item::id, Item::parent)
      .stream()
      .map(Item::id)
      .toList();
  }

  @Test
  void itemsWithoutParentsKeepTheirOrder() {
    assertThat(order(new Item("c", null), new Item("a", null), new Item("b", null)))
      .containsExactly("c", "a", "b")
      .inOrder();
  }

  @Test
  void aParentThatIsAlreadyFirstKeepsTheOrder() {
    assertThat(order(new Item("a", null), new Item("b", "a"), new Item("c", "b")))
      .containsExactly("a", "b", "c")
      .inOrder();
  }

  @Test
  void aChildDeclaredBeforeItsParentIsMovedAfterTheParent() {
    assertThat(
      order(new Item("child", "parent"), new Item("other", null), new Item("parent", null))
    )
      .containsExactly("parent", "child", "other")
      .inOrder();
  }

  @Test
  void aChainDeclaredInReverseIsReversed() {
    assertThat(order(new Item("c", "b"), new Item("b", "a"), new Item("a", null)))
      .containsExactly("a", "b", "c")
      .inOrder();
  }

  @Test
  void severalChildrenOfTheSameParent() {
    assertThat(order(new Item("x", "p"), new Item("y", "p"), new Item("p", null)))
      .containsExactly("p", "x", "y")
      .inOrder();
  }

  @Test
  void anEmptyListIsEmpty() {
    assertThat(order()).isEmpty();
  }

  @Test
  void aParentThatDoesNotExistIsRejectedAndNamed() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      order(new Item("a", null), new Item("b", "missing"))
    );
    assertThat(ex).hasMessageThat().contains("'missing'");
    assertThat(ex).hasMessageThat().contains("'b'");
  }

  @Test
  void anItemThatIsItsOwnParentIsACycle() {
    var ex = assertThrows(IllegalArgumentException.class, () -> order(new Item("a", "a")));
    assertThat(ex).hasMessageThat().contains("cycle");
    assertThat(ex).hasMessageThat().contains("'a' -> 'a'");
  }

  @Test
  void aCycleIsRejectedAndTheCycleIsNamed() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      order(new Item("a", "c"), new Item("b", "a"), new Item("c", "b"), new Item("d", null))
    );
    assertThat(ex).hasMessageThat().contains("cycle");
    assertThat(ex).hasMessageThat().contains("'a'");
    assertThat(ex).hasMessageThat().contains("'b'");
    assertThat(ex).hasMessageThat().contains("'c'");
  }

  @Test
  void duplicateIdsAreRejected() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      order(new Item("a", null), new Item("a", null))
    );
    assertThat(ex).hasMessageThat().contains("'a'");
  }
}
