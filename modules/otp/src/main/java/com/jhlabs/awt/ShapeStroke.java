/*
  This file is based on code copied from project Java Image Processing, JH Labs,
  see the LICENSE file for further information.
*/
package com.jhlabs.awt;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

/**
 * Stroke that draw a shape.
 * <p>
 * Slightly adapted to OTP from original source.
 *
 * @see <a href="http://www.jhlabs.com/java/java2d/strokes/">http://www.jhlabs.com/java/java2d/strokes/</a>
 */
public class ShapeStroke implements Stroke {

  private static final float FLATNESS = 1;
  private final Shape theShape;
  private final float advance;
  private final float phase;
  private final AffineTransform t = new AffineTransform();

  public ShapeStroke(Shape shape, float width, float advance, float phase) {
    this.advance = advance;
    this.phase = phase;
    Rectangle2D bounds = shape.getBounds2D();
    double scale = width / bounds.getHeight();
    t.setToScale(scale, scale);
    t.translate(-bounds.getCenterX(), -bounds.getCenterY());
    this.theShape = t.createTransformedShape(shape);
  }

  public Shape createStrokedShape(Shape shape) {
    GeneralPath result = new GeneralPath();
    PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
    float[] points = new float[6];
    float moveX = 0, moveY = 0;
    float lastX = 0, lastY = 0;
    float thisX = 0, thisY = 0;
    int type = 0;
    float next = phase;

    while (!it.isDone()) {
      type = it.currentSegment(points);
      switch (type) {
        case PathIterator.SEG_MOVETO:
          moveX = lastX = points[0];
          moveY = lastY = points[1];
          result.moveTo(moveX, moveY);
          next = 0;
          break;
        case PathIterator.SEG_CLOSE:
          points[0] = moveX;
          points[1] = moveY;
        // Fall into....

        case PathIterator.SEG_LINETO:
          thisX = points[0];
          thisY = points[1];
          float dx = thisX - lastX;
          float dy = thisY - lastY;
          float distance = (float) Math.sqrt(dx * dx + dy * dy);
          if (distance >= next) {
            float r = 1.0f / distance;
            float angle = (float) Math.atan2(dy, dx);
            while (distance >= next) {
              float x = lastX + next * dx * r;
              float y = lastY + next * dy * r;
              t.setToTranslation(x, y);
              t.rotate(angle);
              result.append(t.createTransformedShape(theShape), false);
              next += advance;
            }
          }
          next -= distance;
          lastX = thisX;
          lastY = thisY;
          break;
      }
      it.next();
    }

    return result;
  }
}
