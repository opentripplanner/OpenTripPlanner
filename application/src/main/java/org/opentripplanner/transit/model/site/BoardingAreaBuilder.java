/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public final class BoardingAreaBuilder
  extends StationElementBuilder<BoardingArea, BoardingAreaBuilder> {

  private RegularStop parentStop;

  BoardingAreaBuilder(FeedScopedId id) {
    super(id);
  }

  BoardingAreaBuilder(BoardingArea original) {
    super(original);
    this.parentStop = original.getParentStop();
  }

  public RegularStop parentStop() {
    return parentStop;
  }

  public BoardingAreaBuilder withParentStop(RegularStop parentStop) {
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
