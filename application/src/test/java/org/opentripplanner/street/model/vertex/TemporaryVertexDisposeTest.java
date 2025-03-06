package org.opentripplanner.street.model.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.edge.FreeEdge;

public class TemporaryVertexDisposeTest {

  private static final double ANY_LOC = 1;

  // Given a very simple graph: A -> B
  private final Vertex a = new V("A");
  private final Vertex b = new V("B");

  {
    edge(a, b);
    assertOriginalGraphIsIntact();
  }

  @Test
  public void disposeNormalCase() {
    // Given a temporary vertex 'origin' and 'destination' connected to graph
    Vertex origin = new TempVertex("origin");
    Vertex destination = new TempVertex("dest");
    edge(origin, a);
    edge(b, destination);

    // Then before we dispose temporary vertexes
    assertEquals("[origin->A]", a.getIncoming().toString());
    assertEquals("[B->dest]", b.getOutgoing().toString());

    // When
    TemporaryVertex.dispose(origin);
    TemporaryVertex.dispose(destination);

    // Then
    assertOriginalGraphIsIntact();
  }

  @Test
  public void disposeShouldNotDeleteOtherIncomingEdges() {
    // Given a temporary vertex 'origin' connected to B - has other incoming edges
    Vertex origin = new TempVertex("origin");
    Vertex otherTemp = new TempVertex("OT");
    edge(origin, b);
    edge(otherTemp, b);

    // Then before we dispose temporary vertexes
    assertEquals("[A->B, origin->B, OT->B]", b.getIncoming().toString());
    // When
    TemporaryVertex.dispose(origin);

    // Then B is back to normal
    assertEquals("[A->B, OT->B]", b.getIncoming().toString());
  }

  @Test
  public void disposeShouldNotDeleteOtherOutgoingEdges() {
    // Given a temporary vertex 'destination' connected from A - with one other outgoing edge
    Vertex destination = new TempVertex("destination");
    Vertex otherTemp = new TempVertex("OT");
    edge(a, destination);
    edge(a, otherTemp);

    // Then before we dispose temporary vertexes
    assertEquals("[A->B, A->destination, A->OT]", a.getOutgoing().toString());

    // When
    TemporaryVertex.dispose(destination);

    // A is back to normal
    assertEquals("[A->B, A->OT]", a.getOutgoing().toString());
  }

  @Test
  public void disposeShouldHandleLoopsInTemporaryPath() {
    Vertex x = new TempVertex("x");
    Vertex y = new TempVertex("y");
    Vertex z = new TempVertex("z");

    edge(x, y);
    edge(y, z);
    edge(z, x);
    // Add some random links the the main graph
    edge(x, a);
    edge(b, y);
    edge(z, a);

    // When
    TemporaryVertex.dispose(x);

    // Then do return without stack overflow and:
    assertOriginalGraphIsIntact();
  }

  /**
   * Verify a complex temporary path is disposed. The temporary graph is connected to the main graph
   * in both directions (in/out) from many places (temp. vertexes).
   * <p>
   * The temporary part of the graph do NOT contain any loops.
   */
  @Test
  public void disposeTemporaryVertexesWithComplexPaths() {
    Vertex x = new TempVertex("x");
    Vertex y = new TempVertex("y");
    Vertex z = new TempVertex("z");
    Vertex q = new TempVertex("q");

    // A and B are connected both ways A->B and B->A (A->B is done in the class init)
    edge(b, a);

    // All temporary vertexes are connected in a chain
    edge(x, y);
    edge(y, z);
    edge(z, q);

    // And they are connected to the graph in many ways - no loops
    edge(x, a);
    edge(y, b);
    edge(z, a);
    edge(q, a);
    edge(q, b);
    edge(a, x);
    edge(b, y);
    edge(a, z);

    // Then before we dispose temporary vertexes
    assertEquals("[B->A, x->A, z->A, q->A]", a.getIncoming().toString());
    assertEquals("[A->B, A->x, A->z]", a.getOutgoing().toString());
    assertEquals("[A->B, y->B, q->B]", b.getIncoming().toString());
    assertEquals("[B->A, B->y]", b.getOutgoing().toString());

    // When
    TemporaryVertex.dispose(x);

    // Then
    assertEquals("[B->A]", a.getIncoming().toString());
    assertEquals("[A->B]", a.getOutgoing().toString());
    assertEquals("[A->B]", b.getIncoming().toString());
    assertEquals("[B->A]", b.getOutgoing().toString());
  }

  /**
   * We should be able to delete an alternative path/loop created in the graph like:
   * <p>
   * A -> x -> y -> B
   * <p>
   * Where 'x' and 'y' are temporary vertexes
   */
  @Test
  public void disposeTemporaryAlternativePath() {
    Vertex x = new TempVertex("x");
    Vertex y = new TempVertex("y");

    // Make a new loop from 'A' via 'x' and 'y' to 'B'
    edge(a, x);
    edge(x, y);
    edge(y, b);

    // Then before we dispose temporary vertexes
    assertEquals("[A->B, A->x]", a.getOutgoing().toString());
    assertEquals("[A->B, y->B]", b.getIncoming().toString());

    // When
    TemporaryVertex.dispose(x);

    // Then
    assertOriginalGraphIsIntact();
  }

  /**
   * We should be able to delete a very deep path without getting a stack overflow error.
   */
  @Test
  public void disposeVeryDeepTemporaryPath() {
    // Create access and egress legs with 1000 vertexes
    Vertex origin = new TempVertex("origin");

    Vertex o1 = origin;
    Vertex o2 = null;

    // Number of temporary vertexes in path
    int i = 1024;

    while (i > 0) {
      o2 = new TempVertex("T" + --i);
      edge(o1, o2);
      o1 = o2;
    }
    edge(o2, a);

    // Verify A is connected to the chain of temporary vertexes.
    assertEquals("[T0->A]", a.getIncoming().toString());

    // When
    TemporaryVertex.dispose(origin);

    // Then
    assertOriginalGraphIsIntact();
  }

  /* private methods */

  // Factory method to create an edge
  private static void edge(Vertex a, Vertex b) {
    E.createE(a, b);
  }

  /* private test helper classes */

  private void assertOriginalGraphIsIntact() {
    assertEquals("[]", a.getIncoming().toString());
    assertEquals("[A->B]", a.getOutgoing().toString());
    assertEquals("[A->B]", b.getIncoming().toString());
    assertEquals("[]", b.getOutgoing().toString());
  }

  private static class V extends Vertex {

    private final String label;

    private V(String label) {
      super(ANY_LOC, ANY_LOC);
      this.label = label;
    }

    @Override
    public String toString() {
      return getLabelString();
    }

    @Override
    public I18NString getName() {
      return NO_NAME;
    }

    @Override
    public VertexLabel getLabel() {
      return VertexLabel.string(label);
    }
  }

  private static class TempVertex extends V implements TemporaryVertex {

    private TempVertex(String label) {
      super(label);
    }
  }

  private static class E extends FreeEdge {

    private E(Vertex from, Vertex to) {
      super(from, to);
    }

    @Override
    public String toString() {
      return getFromVertex().getLabel() + "->" + getToVertex().getLabel();
    }

    private static E createE(Vertex from, Vertex to) {
      return connectToGraph(new E(from, to));
    }
  }
}
