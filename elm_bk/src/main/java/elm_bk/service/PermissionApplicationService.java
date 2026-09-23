package elm_bk.service;

import elm_bk.dto.AuditPermissionDTO;
import elm_bk.dto.BusinessDTO;
import elm_bk.dto.BusinessPermissionDTO;
import elm_bk.entity.PermissionApplication;
import elm_bk.vo.BusinessPermissionVO;
import elm_bk.vo.MerchantApplicationsVO;

import java.util.List;

public interface PermissionApplicationService {
    PermissionApplication applyMerchant();

    PermissionApplication auditApplication(AuditPermissionDTO auditDTO);

    BusinessPermissionVO applyShop(BusinessPermissionDTO businessPermissionDTO);

    BusinessPermissionVO auditShopApplication(BusinessPermissionDTO businessPermissionDTO);

    List<MerchantApplicationsVO> getMerchantApplications();

    List<BusinessPermissionVO> getShopApplications();
}
