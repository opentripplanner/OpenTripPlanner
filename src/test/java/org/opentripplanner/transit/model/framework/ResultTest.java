package org.opentripplanner.transit.model.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResultTest {

  @Test
  public void success() {
    Result<String, Exception> res = Result.success("hello");
    assertTrue(res.isSuccess());
    assertFalse(res.isFailure());

    String[] arr = new String[] { "1" };
    res.ifSuccess(s -> {
      assertEquals("hello", s);
      arr[0] = s;
    });

    assertEquals("hello", arr[0]);

    res.ifFailure(e -> {
      throw new RuntimeException("Should not happen");
    });

    assertEquals("hello", res.successValue());

    Assertions.assertThrows(RuntimeException.class, res::failureValue);
  }

  @Test
  public void failure() {
    var msg = "An error happened";
    Result<String, Exception> res = Result.failure(new RuntimeException(msg));

    assertFalse(res.isSuccess());
    assertTrue(res.isFailure());

    String[] arr = new String[] { "1" };
    res.ifFailure(e -> {
      assertEquals(msg, e.getMessage());
      arr[0] = e.getMessage();
    });

    assertEquals(msg, arr[0]);

    res.ifFailure(e -> {
      assertEquals(msg, e.getMessage());
    });

    assertEquals(msg, res.failureValue().getMessage());

    Assertions.assertThrows(RuntimeException.class, res::successValue);
  }
}
