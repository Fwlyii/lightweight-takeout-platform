package elm_bk.service;

import elm_bk.dto.RiderApplicationDTO;
import elm_bk.dto.RiderAuditDTO;
import elm_bk.entity.RiderProfile;

import java.util.List;

public interface RiderService {
    RiderProfile apply(RiderApplicationDTO dto);

    RiderProfile getMyProfile();

    RiderProfile setOnline(Boolean online);

    List<RiderProfile> listApplications(Integer auditStatus);

    RiderProfile audit(Long applicationId, RiderAuditDTO dto);
}
