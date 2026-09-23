// UserMapper.xml.java
package elm_bk.mapper;

import elm_bk.entity.User;
import elm_bk.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("<script>SELECT u.id,u.username,u.activated,u.create_time,p.phone,p.email,p.photo " +
            "FROM users u LEFT JOIN person p ON p.id=u.id WHERE u.is_deleted=0 " +
            "<if test='status == 1'>AND u.activated=1 </if>" +
            "<if test='status == 2'>AND u.activated=0 </if>" +
            "<if test='keyword != null and keyword != &quot;&quot;'>" +
            "AND (u.username LIKE CONCAT('%',#{keyword},'%') OR p.phone LIKE CONCAT('%',#{keyword},'%') " +
            "OR p.email LIKE CONCAT('%',#{keyword},'%')) </if>ORDER BY u.id</script>")
    List<elm_bk.vo.AdminUserVO> listAdminUsers(@Param("status") int status, @Param("keyword") String keyword);
    @org.apache.ibatis.annotations.Select("SELECT id FROM users WHERE id=#{id} FOR UPDATE")
    Long lockAccount(Long id);
    @Select("SELECT * FROM users WHERE id = #{id} AND is_deleted = 0")
    User findById(Long id);
    @Select("SELECT * FROM users WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(String username);
    User findByUsernameWithAuthorities(String username);
    User findByPhoneWithAuthorities(String phone);
    User findByUserIdWithAuthorities(Long userId);
    void insert(User user);
    void update(User user);
    @Select("SELECT COUNT(*) FROM users WHERE is_deleted = 0")
    int count();

    @Select("SELECT COUNT(*) FROM users WHERE is_deleted = 0 AND create_time >= #{from} AND create_time < #{to}")
    int countCreatedBetween(@Param("from") java.time.LocalDateTime from,
                            @Param("to") java.time.LocalDateTime to);
    @Insert("INSERT INTO user_authority (user_id, authority_name) VALUES (#{userId}, #{authorityName})")
    void insertUserAuthority(@Param("userId") Long userId, @Param("authorityName") String authorityName);

    @Select("select id from users where username = #{username}")
    Long getUserIdByUsername(String username);

    @Select("SELECT * FROM users WHERE id = #{userId} AND is_deleted = 0")
    Integer countUserById(@Param("userId") Long userId);

    @Update("UPDATE users SET activated = #{activated} WHERE id = #{id}")
    void updateActivated(User user);

    @Select("SELECT ua.user_id FROM user_authority ua WHERE ua.authority_name = #{authorityName}")
    List<Long> findUserIdsByAuthority(@Param("authorityName") String authorityName);
}
