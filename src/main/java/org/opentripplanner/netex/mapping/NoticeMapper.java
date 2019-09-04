package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.impl.EntityById;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * Maps NeTEx notice to OTP notice.
 * <p/>
 * This Mapper is stateful, it caches objects it already have mapped. Because
 * of this just one instance of the mapper should be used in a context where
 * the same Notice may appear more than once.
 */
class NoticeMapper {

    private EntityById<FeedScopedId, Notice> cache = new EntityById<>();

    NoticeMapper() {}

    Notice map(org.rutebanken.netex.model.Notice netexNotice) {
            FeedScopedId id = createFeedScopedId(netexNotice.getId());
            Notice otpNotice = cache.get(id);

            if(otpNotice == null) {
                otpNotice = new Notice();

                otpNotice.setId(id);
                otpNotice.setText(netexNotice.getText().getValue());
                otpNotice.setPublicCode(netexNotice.getPublicCode());
                cache.add(otpNotice);
            }
            return otpNotice;

    }
}
