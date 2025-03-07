package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Maps NeTEx notice to OTP notice.
 * <p/>
 * This Mapper is stateful, it caches objects it already have mapped. Because of this just one
 * instance of the mapper should be used in a context where the same Notice may appear more than
 * once.
 */
class NoticeMapper {

  private final FeedScopedIdFactory idFactory;

  private final EntityById<Notice> cache = new DefaultEntityById<>();

  NoticeMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Notice map(org.rutebanken.netex.model.Notice netexNotice) {
    FeedScopedId id = idFactory.createId(netexNotice.getId());
    Notice otpNotice = cache.get(id);

    if (otpNotice == null) {
      otpNotice = Notice.of(id)
        .withPublicCode(netexNotice.getPublicCode())
        .withText(netexNotice.getText().getValue())
        .build();

      cache.add(otpNotice);
    }
    return otpNotice;
  }
}
