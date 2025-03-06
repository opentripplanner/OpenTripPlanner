/*
  This file is based on code copied from project Java Image Processing, JH Labs,
  see the LICENSE file for further information.
*/
package com.jhlabs.awt;

import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

/**
 * Stroke that paint a text.
 * <p>
 * Slightly adapted to OTP from original source.
 *
 * @see <a href="http://www.jhlabs.com/java/java2d/strokes/">http://www.jhlabs.com/java/java2d/strokes/</a>
 */
public class TextStroke implements Stroke {

  private static final float FLATNESS = 1;
  private final String text;
  private final Font font;
  private final AffineTransform t = new AffineTransform();
  private boolean stretchToFit = false;
  private boolean repeat = false;

  public TextStroke(String text, Font font) {
    this(text, font, true, false);
  }

  public TextStroke(String text, Font font, boolean stretchToFit, boolean repeat) {
    this.text = text;
    this.font = font;
    this.stretchToFit = stretchToFit;
    this.repeat = repeat;
  }

  public Shape createStrokedShape(Shape shape) {
    FontRenderContext frc = new FontRenderContext(null, true, true);
    GlyphVector glyphVector = font.createGlyphVector(frc, text);

    GeneralPath result = new GeneralPath();
    PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
    float[] points = new float[6];
    float moveX = 0, moveY = 0;
    float lastX = 0, lastY = 0;
    float thisX = 0, thisY = 0;
    int type = 0;
    float next = 0;
    int currentChar = 0;
    int length = glyphVector.getNumGlyphs();

    if (length == 0) return result;

    float factor = stretchToFit
      ? measurePathLength(shape) / (float) glyphVector.getLogicalBounds().getWidth()
      : 1.0f;
    float height = (float) glyphVector.getLogicalBounds().getHeight();
    float nextAdvance = 0;

    while (currentChar < length && !it.isDone()) {
      type = it.currentSegment(points);
      switch (type) {
        case PathIterator.SEG_MOVETO:
          moveX = lastX = points[0];
          moveY = lastY = points[1];
          result.moveTo(moveX, moveY);
          nextAdvance = glyphVector.getGlyphMetrics(currentChar).getAdvance() * 0.5f;
          next = nextAdvance;
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
            while (currentChar < length && distance >= next) {
              Shape glyph = glyphVector.getGlyphOutline(currentChar);
              Point2D p = glyphVector.getGlyphPosition(currentChar);
              float px = (float) p.getX();
              float py = (float) p.getY();
              float x = lastX + next * dx * r;
              float y = lastY + next * dy * r;
              float advance = nextAdvance;
              nextAdvance = currentChar < length - 1
                ? glyphVector.getGlyphMetrics(currentChar + 1).getAdvance() * 0.5f
                : 0;
              t.setToTranslation(x, y);
              t.rotate(angle);
              t.translate(-px - advance, -py + (height * factor) / 2.0f);
              result.append(t.createTransformedShape(glyph), false);
              next += (advance + nextAdvance) * factor;
              currentChar++;
              if (repeat) currentChar %= length;
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

  public float measurePathLength(Shape shape) {
    PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
    float[] points = new float[6];
    float moveX = 0, moveY = 0;
    float lastX = 0, lastY = 0;
    float thisX = 0, thisY = 0;
    int type = 0;
    float total = 0;

    while (!it.isDone()) {
      type = it.currentSegment(points);
      switch (type) {
        case PathIterator.SEG_MOVETO:
          moveX = lastX = points[0];
          moveY = lastY = points[1];
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
          total += (float) Math.sqrt(dx * dx + dy * dy);
          lastX = thisX;
          lastY = thisY;
          break;
      }
      it.next();
    }

    return total;
  }
}
