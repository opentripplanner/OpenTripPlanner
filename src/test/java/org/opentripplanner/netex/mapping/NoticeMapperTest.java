package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Notice;

import static org.junit.Assert.assertEquals;

public class NoticeMapperTest {

    private static final String NOTICE_ID = "RUT:Notice:1";
    private static final String NOTICE_TEXT = "Something is happening on this line";
    private static final String PUBLIC_CODE = "Public Code";

    @Test public void mapNotice() {
        org.opentripplanner.model.Notice otpNotice;

        // Given
        NoticeMapper mapper = new NoticeMapper();
        // And
        Notice netexNotice = new Notice();
        netexNotice.setId(NOTICE_ID);
        netexNotice.setText(new MultilingualString().withValue(NOTICE_TEXT));
        netexNotice.setPublicCode(PUBLIC_CODE);

        // When
        otpNotice = mapper.map(netexNotice);

        // Then
        assertEquals(NOTICE_ID, otpNotice.getId().getId());
        assertEquals(NOTICE_TEXT, otpNotice.getText());
        assertEquals(PUBLIC_CODE, otpNotice.getPublicCode());

        // And when other instance with same id is mapped, the first one is returned
        // from cache - ignoring all properties except the id
        otpNotice = mapper.map(
                new Notice()
                        .withId(NOTICE_ID)
                        .withPublicCode("Albatross")
                        .withText(new MultilingualString().withValue("Different text"))
        );

        // Then
        assertEquals(NOTICE_ID, otpNotice.getId().getId());
        assertEquals("Not Albatross", NOTICE_TEXT, otpNotice.getText());
    }
}
