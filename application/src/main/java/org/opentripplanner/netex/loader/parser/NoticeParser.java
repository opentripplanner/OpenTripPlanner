package org.opentripplanner.netex.loader.parser;

import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import org.entur.netex2.model.DataManagedObjectStructure;
import org.entur.netex2.model.Notice;
import org.entur.netex2.model.NoticeAssignment;
import org.entur.netex2.model.NoticeAssignmentsInFrame_RelStructure;
import org.entur.netex2.model.NoticesInFrame_RelStructure;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse a Notice and Notice Assigment, used only be the {@link ServiceFrameParser} and
 * {@link TimeTableFrameParser}.
 */
class NoticeParser {

  private static final Logger LOG = LoggerFactory.getLogger(NoticeParser.class);

  private final Collection<Notice> notices = new ArrayList<>();
  private final Collection<NoticeAssignment> noticeAssignments = new ArrayList<>();

  void parseNotices(NoticesInFrame_RelStructure notices) {
    if (notices == null) {
      return;
    }

    this.notices.addAll(notices.getNotice());
  }

  void parseNoticeAssignments(NoticeAssignmentsInFrame_RelStructure na) {
    if (na == null) {
      return;
    }

    for (JAXBElement<? extends DataManagedObjectStructure> it : na.getNoticeAssignment()) {
      NoticeAssignment noticeAssignment = (NoticeAssignment) it.getValue();
      boolean error = false;

      if (noticeAssignment.getNoticedObjectRef() == null) {
        LOG.warn(
          "Notice assignment is missing 'noticedObjectRef'. Id: {}",
          noticeAssignment.getId()
        );
        error = true;
      }
      if (noticeAssignment.getNoticeRef() == null && noticeAssignment.getNotice() == null) {
        LOG.warn(
          "Notice assignment have no 'notice' or 'noticeRef'. Id: {}",
          noticeAssignment.getId()
        );
        error = true;
      }
      if (!error) {
        this.noticeAssignments.add(noticeAssignment);
      }
    }
  }

  void setResultOnIndex(NetexEntityIndex index) {
    // update entities
    index.noticeById.addAll(notices);
    index.noticeAssignmentById.addAll(noticeAssignments);
  }
}
