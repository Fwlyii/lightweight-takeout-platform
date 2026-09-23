package elm_bk.service;

import elm_bk.dto.*;
import elm_bk.entity.*;
import elm_bk.mapper.*;
import elm_bk.service.impl.*;
import elm_bk.vo.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/** Regression contracts for the eight-issue audit. */
class AuditRegressionTest {
    private static User merchant() {
        var user=new User(); user.setId(20L);
        var authority=new Authority(); authority.setName("BUSINESS"); user.setAuthorities(List.of(authority));
        return user;
    }

    @Test void productEditsLockBeforeReadingStock() throws Exception {
        var method=FoodMapper.class.getMethod("lockFoodById",Long.class);
        assertTrue(method.getAnnotation(org.apache.ibatis.annotations.Select.class).value()[0].toUpperCase().contains("FOR UPDATE"));
        var food=new Food(); food.setId(1L); food.setBusinessId(2L); food.setFoodName("old");
        food.setFoodPrice(BigDecimal.TEN);food.setStock(9);
        var foods=mock(FoodMapper.class, invocation -> invocation.getMethod().getName().contains("FoodById") ? food : org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation));
        var businesses=mock(BusinessMapper.class);var current=mock(CurrentUserService.class);
        var business=new Business();business.setUserId(20L);
        when(businesses.selectBusinessById(2L)).thenReturn(business);when(current.requireUser()).thenReturn(merchant());
        var service=new FoodServiceImpl(foods,businesses,mock(OrdersMapper.class),current);
        var update=new FoodUpdateDTO();update.setFoodId(1L);update.setFoodName("new");service.modifyFoodMessage(update);
        assertTrue(mockingDetails(foods).getInvocations().stream().anyMatch(i->i.getMethod().getName().equals("lockFoodById")));
        verify(foods).updateFoodMessage(argThat(f->f.getStock()==9));
    }

    @Test void deletionMustGuardActiveOrdersAndSerializeWithSubmission() throws Exception {
        String sql=BusinessMapper.class.getMethod("deleteBusiness",Long.class).getAnnotation(org.apache.ibatis.annotations.Update.class).value()[0];
        assertTrue(sql.toLowerCase().contains("not exists"));
        assertTrue(sql.contains("7") && sql.contains("8"));
        var lock=BusinessMapper.class.getMethod("lockBusinessById",Long.class);
        assertTrue(lock.getAnnotation(org.apache.ibatis.annotations.Select.class).value()[0].toUpperCase().contains("FOR UPDATE"));
    }

    private Configuration mapper(String name) throws Exception {
        var config=new Configuration();
        String resource="elm_bk/mapper/"+name+".xml";
        try(var stream=getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream,config,resource,config.getSqlFragments()).parse();
        }
        return config;
    }

    @Test void clearingPromotionExplicitlyWritesBothColumnsToNull() throws Exception {
        var config=mapper("BusinessMapper"); var update=new HashMap<String,Object>();
        update.put("promotionEnabled",false);update.put("updater",20L);
        var sql=config.getMappedStatement("elm_bk.mapper.BusinessMapper.patchBusiness")
            .getBoundSql(Map.of("id",2L,"updateDto",update)).getSql();
        assertTrue(sql.contains("promotion_threshold = NULL"));assertTrue(sql.contains("promotion_discount = NULL"));
    }

    @Test void hiddenReviewRemainsVisibleToOwnerButNotPublic() throws Exception {
        var config=mapper("ReviewMapper");
        var dataSource=new UnpooledDataSource("org.h2.Driver","jdbc:h2:mem:audit_review;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        config.setEnvironment(new Environment("audit",new JdbcTransactionFactory(),dataSource));
        try(var connection=dataSource.getConnection();var statement=connection.createStatement()) {
            statement.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,username VARCHAR(30))");
            statement.execute("CREATE TABLE business(id BIGINT PRIMARY KEY,business_name VARCHAR(30))");
            statement.execute("CREATE TABLE review(id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id BIGINT UNIQUE,customer_id BIGINT,business_id BIGINT,rating INT,content VARCHAR(500),images VARCHAR(500),merchant_reply VARCHAR(500),reply_time TIMESTAMP,create_time TIMESTAMP,update_time TIMESTAMP,is_hidden INT DEFAULT 0)");
            statement.execute("INSERT INTO users VALUES(1,'test')");statement.execute("INSERT INTO business VALUES(2,'shop')");
            statement.execute("INSERT INTO review(order_id,customer_id,business_id,rating,is_hidden) VALUES(3,1,2,5,1)");
        }
        try(var session=new SqlSessionFactoryBuilder().build(config).openSession()) {
            var reviews=session.getMapper(ReviewMapper.class);
            assertNotNull(reviews.selectByOrderId(3L));
            assertTrue(reviews.selectByOrderId(3L).getHidden());
            assertTrue(reviews.listByBusiness(2L).isEmpty());
            var repeat=new Review();repeat.setOrderId(3L);repeat.setCustomerId(1L);repeat.setBusinessId(2L);repeat.setRating(5);
            assertThrows(org.apache.ibatis.exceptions.PersistenceException.class,()->reviews.insert(repeat));
        }
    }
    @Test void monthlySalesExcludeDemoBaselineAndOldOrders() throws Exception {
        var config=mapper("BusinessMapper");
        String sql=config.getMappedStatement("elm_bk.mapper.BusinessMapper.searchBusinesses")
            .getBoundSql(new HashMap<>()).getSql();
        assertFalse(sql.contains("COALESCE(b.demo_sales_count"));
        assertTrue(sql.contains("TIMESTAMPADD(DAY, -30, CURRENT_TIMESTAMP)"));
    }

    @Test void databaseRefusesDeletionForEveryUnfinishedOrderState() throws Exception {
        var config=mapper("BusinessMapper");
        var ds=new UnpooledDataSource("org.h2.Driver","jdbc:h2:mem:audit_delete;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        config.setEnvironment(new Environment("audit",new JdbcTransactionFactory(),ds));
        try(var c=ds.getConnection();var s=c.createStatement()) {
            s.execute("CREATE TABLE business(id BIGINT PRIMARY KEY,is_deleted INT)");
            s.execute("CREATE TABLE orders(business_id BIGINT,order_state INT,is_deleted INT)");
            for(int state=0;state<=9;state++) {
                s.execute("INSERT INTO business VALUES("+state+",0)");
                s.execute("INSERT INTO orders VALUES("+state+","+state+",0)");
            }
        }
        try(var session=new SqlSessionFactoryBuilder().build(config).openSession()) {
            var businesses=session.getMapper(BusinessMapper.class);
            for(int state=0;state<=9;state++)assertEquals(state==7 || state==8 ? 1 : 0,businesses.deleteBusiness((long)state));
        }
    }

    @Test void actualSalesQueryCountsOnlyRecentCompletedOrders() throws Exception {
        var config=mapper("BusinessMapper");
        String sql=config.getMappedStatement("elm_bk.mapper.BusinessMapper.searchBusinesses").getBoundSql(new HashMap<>()).getSql();
        int start=sql.indexOf("(SELECT COUNT(*)");
        String count=sql.substring(start+1,sql.indexOf(") as salesCount",start));
        var ds=new UnpooledDataSource("org.h2.Driver","jdbc:h2:mem:audit_sales;MODE=MySQL","sa","");
        try(var c=ds.getConnection();var s=c.createStatement()) {
            s.execute("CREATE TABLE business(id BIGINT PRIMARY KEY)");s.execute("INSERT INTO business VALUES(2)");
            s.execute("CREATE TABLE orders(business_id BIGINT,order_state INT,is_deleted INT,order_date TIMESTAMP)");
            s.execute("INSERT INTO orders VALUES(2,7,0,CURRENT_TIMESTAMP),(2,7,0,TIMESTAMPADD(DAY,-60,CURRENT_TIMESTAMP)),(2,8,0,CURRENT_TIMESTAMP),(2,7,1,CURRENT_TIMESTAMP)");
            try(var rows=s.executeQuery("SELECT ("+count+") FROM business b")) {assertTrue(rows.next());assertEquals(1,rows.getInt(1));}
        }
    }
}
