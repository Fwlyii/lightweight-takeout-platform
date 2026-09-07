package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.DeliveryExceptionResolveDTO;
import com.tju.elm_bk.dto.RiderAuditDTO;
import com.tju.elm_bk.entity.RiderProfile;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.DeliveryService;
import com.tju.elm_bk.service.RiderService;
import com.tju.elm_bk.vo.DeliveryExceptionVO;
import com.tju.elm_bk.vo.DeliveryTaskVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import com.tju.elm_bk.exception.APIException;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminRiderController {
    private final RiderService riderService;
    private final ObjectProvider<DeliveryService> deliveryService;

    public AdminRiderController(RiderService riderService, ObjectProvider<DeliveryService> deliveryService) {
        this.riderService = riderService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/rider-applications")
    public HttpResult<List<RiderProfile>> riderApplications(@RequestParam(required = false) Integer status) {
        return HttpResult.success(riderService.listApplications(status));
    }

    @PostMapping("/rider-applications/{id}/audit")
    public HttpResult<RiderProfile> auditRider(@PathVariable Long id, @Valid @RequestBody RiderAuditDTO dto) {
        return HttpResult.success(riderService.audit(id, dto));
    }

    @GetMapping("/delivery-exceptions")
    public HttpResult<List<DeliveryExceptionVO>> deliveryExceptions(@RequestParam(required = false) Integer status) {
        return HttpResult.success(requireDeliveryService().listExceptions(status));
    }

    @PostMapping("/delivery-exceptions/{id}/resolve")
    public HttpResult<DeliveryTaskVO> resolveException(@PathVariable Long id,
                                                       @Valid @RequestBody DeliveryExceptionResolveDTO dto) {
        return HttpResult.success(requireDeliveryService().resolveException(id, dto));
    }

    private DeliveryService requireDeliveryService() {
        DeliveryService service = deliveryService.getIfAvailable();
        if (service == null) {
            throw new APIException("FEATURE_UNAVAILABLE", "配送模块尚未启用");
        }
        return service;
    }
}

