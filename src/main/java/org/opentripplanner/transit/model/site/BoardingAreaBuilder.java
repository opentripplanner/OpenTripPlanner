/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public final class BoardingAreaBuilder
  extends StationElementBuilder<BoardingArea, BoardingAreaBuilder> {

  private Stop parentStop;

  public BoardingAreaBuilder(FeedScopedId id) {
    super(id);
  }

  public BoardingAreaBuilder(BoardingArea original) {
    super(original);
    this.parentStop = original.getParentStop();
  }

  public Stop parentStop() {
    return parentStop;
  }

  public BoardingAreaBuilder withParentStop(Stop parentStop) {
    this.parentStop = parentStop;
    return this;
  }

  @Override
  BoardingAreaBuilder instance() {
    return this;
  }

  @Override
  protected BoardingArea buildFromValues() {
    return new BoardingArea(this);
  }
}
