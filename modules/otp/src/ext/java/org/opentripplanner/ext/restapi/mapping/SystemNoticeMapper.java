package org.opentripplanner.ext.restapi.mapping;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.restapi.model.ApiSystemNotice;
import org.opentripplanner.model.SystemNotice;

public final class SystemNoticeMapper {

  private SystemNoticeMapper() {}

  public static List<ApiSystemNotice> mapSystemNotices(Collection<SystemNotice> domainList) {
    // Using {@code null} and not an empty set will minimize the JSON removing the
    // {@code systemNotices} from the result.
    if (domainList == null || domainList.isEmpty()) {
      return null;
    }

    return domainList
      .stream()
      .map(SystemNoticeMapper::mapSystemNotice)
      .collect(Collectors.toList());
  }

  public static ApiSystemNotice mapSystemNotice(SystemNotice domain) {
    return new ApiSystemNotice(domain.tag(), domain.text());
  }
}
