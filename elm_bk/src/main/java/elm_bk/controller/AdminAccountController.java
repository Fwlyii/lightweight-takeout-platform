package elm_bk.controller;

import elm_bk.result.HttpResult;
import elm_bk.service.AdminAccountService;
import elm_bk.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminAccountController {
    private final AdminAccountService accounts;

    @GetMapping
    public HttpResult<List<AdminUserVO>> list(@RequestParam(defaultValue = "0") int status,
            @RequestParam(defaultValue = "") String keyword) {
        return HttpResult.success(accounts.list(status, keyword));
    }

    @PutMapping("/{userId}/status")
    public HttpResult<Void> setActivated(@PathVariable Long userId, @RequestParam boolean activated) {
        accounts.setActivated(userId, activated);
        return HttpResult.success();
    }
}
