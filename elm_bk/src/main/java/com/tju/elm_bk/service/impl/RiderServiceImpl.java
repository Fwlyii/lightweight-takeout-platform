package com.tju.elm_bk.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tju.elm_bk.constants.RiderAuditStatus;
import com.tju.elm_bk.dto.RiderApplicationDTO;
import com.tju.elm_bk.dto.RiderAuditDTO;
import com.tju.elm_bk.entity.Notification;
import com.tju.elm_bk.entity.RiderProfile;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.*;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.RiderService;
import com.tju.elm_bk.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {
    private final RiderMapper riderMapper;
    private final AuthorityMapper authorityMapper;
    private final UserAuthorityMapper userAuthorityMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationDispatcher notifications;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public RiderProfile apply(RiderApplicationDTO dto) {
        User current = currentUser();
        RiderProfile existing = riderMapper.findByUserId(current.getId());
        if (existing != null && existing.getAuditStatus() == RiderAuditStatus.PENDING) {
            throw new APIException("骑手申请正在审核中，请勿重复提交");
        }
        if (existing != null && existing.getAuditStatus() == RiderAuditStatus.APPROVED) {
            throw new APIException("您已经是审核通过的骑手");
        }

        RiderProfile profile = new RiderProfile();
        profile.setUserId(current.getId());
        profile.setRealName(dto.getRealName().trim());
        profile.setPhone(dto.getPhone().trim());
        profile.setVehicleType(dto.getVehicleType());
        profile.setAuditStatus(RiderAuditStatus.PENDING);
        profile.setOnline(false);

        if (existing == null) {
            riderMapper.insert(profile);
        } else if (riderMapper.resubmit(profile) != 1) {
            throw new APIException("骑手申请重新提交失败");
        }

        JSONObject message = new JSONObject();
        message.put("type", "rider_application");
        message.put("content", "用户[" + current.getUsername() + "]提交了骑手申请");
        notifications.sendToAuthority("ADMIN", message.toJSONString());
        return riderMapper.findByUserId(current.getId());
    }

    @Override
    public RiderProfile getMyProfile() {
        return riderMapper.findByUserId(currentUser().getId());
    }

    @Override
    @Transactional
    public RiderProfile setOnline(Boolean online) {
        User current = currentUser();
        RiderProfile profile = riderMapper.findByUserId(current.getId());
        if (profile == null || profile.getAuditStatus() != RiderAuditStatus.APPROVED) {
            throw new APIException("骑手身份尚未审核通过");
        }
        if (!online && deliveryTaskMapper.countActiveByRider(current.getId()) > 0) {
            throw new APIException("存在进行中的配送任务，暂时不能下线");
        }
        if (riderMapper.updateOnline(current.getId(), online) != 1) {
            throw new APIException("更新骑手在线状态失败");
        }
        return riderMapper.findByUserId(current.getId());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<RiderProfile> listApplications(Integer auditStatus) {
        if (auditStatus != null && (auditStatus < 0 || auditStatus > 2)) {
            throw new APIException("不支持的审核状态");
        }
        return riderMapper.listApplications(auditStatus);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public RiderProfile audit(Long applicationId, RiderAuditDTO dto) {
        if (applicationId == null || applicationId <= 0 || dto == null || dto.getApproved() == null
                || (dto.getReason() != null && dto.getReason().length() > 200)) {
            throw new APIException("骑手审核参数不合法");
        }
        RiderProfile application = riderMapper.findById(applicationId);
        if (application == null) {
            throw new APIException("骑手申请不存在");
        }
        if (application.getAuditStatus() != RiderAuditStatus.PENDING) {
            throw new APIException("该骑手申请已经审核");
        }
        if (!dto.getApproved() && (dto.getReason() == null || dto.getReason().isBlank())) {
            throw new APIException("拒绝申请时必须填写原因");
        }

        int targetStatus = dto.getApproved() ? RiderAuditStatus.APPROVED : RiderAuditStatus.REJECTED;
        String reason = dto.getApproved() ? null : dto.getReason().trim();
        if (riderMapper.audit(applicationId, targetStatus, reason) != 1) {
            throw new APIException("骑手审核失败，请刷新后重试");
        }
        if (dto.getApproved()) {
            if (authorityMapper.findByName("RIDER") == null) {
                throw new APIException("系统缺少 RIDER 权限配置");
            }
            if (userAuthorityMapper.countByUserIdAndAuthority(application.getUserId(), "RIDER") == 0) {
                userAuthorityMapper.insertUserAuthority(application.getUserId(), "RIDER");
            }
        }
        sendAuditNotification(application.getUserId(), dto.getApproved(), reason);
        return riderMapper.findById(applicationId);
    }

    private User currentUser() {
        return currentUserService.requireUser();
    }

    private void sendAuditNotification(Long userId, boolean approved, String reason) {
        String content = approved
                ? "骑手申请已通过，重新登录后即可进入骑手工作台"
                : "骑手申请未通过：" + reason;
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotificationType(2);
        notification.setNotificationContent(content);
        notification.setAuditResult(approved ? 1 : 2);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notification.setIsDeleted(0);
        notificationMapper.insert(notification);

        JSONObject message = new JSONObject();
        message.put("type", "rider_audit");
        message.put("content", content);
        notifications.sendToClient(userId.toString(), message.toJSONString());
    }
}
