package com.tju.elm_bk.mapper;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatHistoryMapperAuthorizationTest {
    @Test
    void deleteMustIncludeOwnerAndSoftDeletePredicates() throws Exception {
        Method method = AiChatHistoryMapper.class.getMethod("deleteById", Long.class, Long.class);
        String sql = String.join(" ", method.getAnnotation(Update.class).value());

        assertTrue(sql.contains("user_id = #{updater}"));
        assertTrue(sql.contains("is_deleted = 0"));
    }
}
