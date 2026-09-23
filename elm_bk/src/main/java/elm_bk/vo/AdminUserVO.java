package elm_bk.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** Explicit account-list projection: never serialize credentials. */
@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String photo;
    private Boolean activated;
    private LocalDateTime createTime;
}
