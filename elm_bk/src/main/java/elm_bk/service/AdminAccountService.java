package elm_bk.service;

import elm_bk.exception.APIException;
import elm_bk.mapper.UserMapper;
import elm_bk.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Account administration rules, independent of HTTP parameter binding. */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminAccountService {
    private final UserMapper users;
    private final CurrentUserService currentUser;

    public List<AdminUserVO> list(int status, String keyword) {
        currentUser.requireUser();
        if (status < 0 || status > 2 || keyword == null || keyword.length() > 100) {
            throw new APIException("查询条件无效");
        }
        return users.listAdminUsers(status, keyword.trim());
    }

    @Transactional
    public void setActivated(Long userId, boolean activated) {
        Long operatorId = currentUser.requireUserId();
        users.lockAccount(userId);
        var account = users.findByUserIdWithAuthorities(userId);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted())) throw new APIException("用户不存在");
        if (!activated && (operatorId.equals(userId) || currentUser.isAdmin(account))) {
            throw new APIException("不能禁用自己或管理员账户");
        }
        account.setActivated(activated);
        users.updateActivated(account);
    }
}
