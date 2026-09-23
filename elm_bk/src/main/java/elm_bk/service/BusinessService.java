package elm_bk.service;


import elm_bk.dto.BusinessDTO;
import elm_bk.dto.BusinessInfoDTO;
import elm_bk.dto.BusinessUpdateDTO;

import elm_bk.entity.Business;
import elm_bk.vo.BusinessSearchVO;
import elm_bk.vo.BusinessVO;
import elm_bk.vo.MerchantStatsVO;

import java.util.List;

public interface BusinessService {
    BusinessVO getBusinessById(Long id);

    /** 单家店铺的展示指标（评分/月售/人均/推荐标签）。 */
    BusinessSearchVO getBusinessSummary(Long id);
    BusinessVO updateBusiness(Long id, BusinessUpdateDTO updateDto);
    BusinessVO deleteBusiness(Long id);
    BusinessVO patchBusiness(Long id, BusinessUpdateDTO updateDto);
    List<BusinessVO> getBusinesses();
    BusinessVO addBusiness(BusinessDTO businessDto);
    List<BusinessSearchVO> getBusinessesBySearch(String keyword, boolean isScore,boolean isSales);
    Integer applyForAddBusiness(Business  business);
    List<BusinessInfoDTO> getAllActiveBusinesses();
    List<Business> getMerchantBusinesses(Long userId, Integer status);
    List<Business>listBusinessByOrderTypeId(Integer type);
    List<MerchantStatsVO> getBusinessIdList();
    List<BusinessSearchVO> getBusinessesInCarousel();
    public BusinessVO patchBusinessOwn(Long id, BusinessUpdateDTO updateDto);
}
