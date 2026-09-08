package com.tju.elm_bk.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Feature acceptance through HTTP, real security, services and database; not source-marker checks. */
@SpringBootTest(properties = {
 "spring.datasource.url=jdbc:h2:mem:profile_journey;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
 "spring.datasource.password=",
 "spring.sql.init.schema-locations=classpath:auth-schema.sql,classpath:profile-schema.sql"
})
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
@WithMockUser(username = "owner", authorities = "USER")
class ProfileAddressJourneyTest {
 @Test
 void parallelFirstAddressesHaveExactlyOneDefault() throws Exception {
  runConcurrent(() -> mvc.perform(post("/api/addresses/me")
   .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("owner").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER")))
   .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(address()))).andReturn().getResponse().getStatus());
  assertEquals(6,jdbc.queryForObject("SELECT COUNT(*) FROM delivery_address WHERE user_id=1 AND is_deleted=0",Integer.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM delivery_address WHERE user_id=1 AND is_deleted=0 AND is_default=1",Integer.class));
 }

 @Test
 void parallelFavoritesKeepOneRelationship() throws Exception {
  runConcurrent(() -> mvc.perform(post("/api/merchant/interaction/update")
   .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("owner").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER")))
   .contentType(MediaType.APPLICATION_JSON).content("{\"merchantId\":1,\"collected\":true}")).andReturn().getResponse().getStatus());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM merchant_interaction WHERE user_id=1 AND collected=1",Integer.class));
 }

 private void runConcurrent(java.util.concurrent.Callable<Integer> action) throws Exception {
  var executor=java.util.concurrent.Executors.newFixedThreadPool(6);
  var start=new java.util.concurrent.CountDownLatch(1);
  var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
  try {
   for(int i=0;i<6;i++)futures.add(executor.submit(()->{start.await();return action.call();}));
   start.countDown();
   for(var future:futures)assertEquals(200,future.get(30,java.util.concurrent.TimeUnit.SECONDS));
  } finally {executor.shutdownNow();}
 }

 @Test
 @org.springframework.security.test.context.support.WithAnonymousUser
 void anonymousCannotReadPersonalData() throws Exception {
  for(String path:new String[]{"/api/user","/api/addresses/me","/api/merchant/interaction/collections/me"})
   mvc.perform(get(path)).andExpect(status().isUnauthorized());
 }

 @Test
 void favoritesReflectNewReviewsWithoutResavingFavorite() throws Exception {
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON).content("{\"merchantId\":1,\"collected\":true}")).andExpect(status().isOk());
  jdbc.update("INSERT INTO review(business_id,rating,is_hidden) VALUES (1,5,0)");
  mvc.perform(get("/api/merchant/interaction/collections/me")).andExpect(jsonPath("$.data[0].score").value(4.67));
 }
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 @Autowired ObjectMapper json;

 @BeforeEach
 void seed() {
  for (String table : new String[]{"delivery_address", "merchant_interaction", "review", "orders",
    "business", "user_authority", "person", "users", "authority"}) jdbc.update("DELETE FROM " + table);
  jdbc.update("INSERT INTO authority(name) VALUES ('USER'), ('RIDER')");
  jdbc.update("INSERT INTO users(id,username,password,activated,is_deleted) VALUES (1,'owner','test-only',1,0),(2,'other','test-only',1,0)");
  jdbc.update("INSERT INTO user_authority(user_id,authority_name) VALUES (1,'USER'),(2,'USER')");
  jdbc.update("INSERT INTO person(id,phone,photo) VALUES (1,'13900000001','/images/default-user-avatar.png'),(2,'13900000002','/images/default-user-avatar.png')");
  jdbc.update("INSERT INTO business(id,business_name,start_price,delivery_price,demo_rating,demo_sales_count,status,is_deleted) VALUES (1,'测试食堂',20,3,1.36,500,1,0),(2,'未审核店',20,3,5,0,0,0)");
  jdbc.update("INSERT INTO review(business_id,rating,is_hidden) VALUES (1,5,0),(1,4,0),(1,1,1)");
 }

 private Map<String,Object> address() {
  return new LinkedHashMap<>(Map.of("contactName","李同学","contactSex",1,"contactTel","13900000001","address","天津大学北洋园校区正园9斋"));
 }
 private long createAddress() throws Exception {
  String result = mvc.perform(post("/api/addresses/me").contentType(MediaType.APPLICATION_JSON)
    .content(json.writeValueAsString(address()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  return json.readTree(result).path("data").path("id").asLong();
 }
 private void otherAddress() {
  jdbc.update("INSERT INTO delivery_address(id,user_id,contact_name,contact_tel,address,is_deleted,is_default) VALUES (999,2,'别人','13900000002','他人地址',0,1)");
 }
 private Map<String,Object> profile() {
  return new LinkedHashMap<>(Map.of("firstName","李","lastName","同学","phone","13900000003","email","student@example.test","gender","男"));
 }

 @Test void addressCrudAndFirstDefaultUseServerIdentity() throws Exception {
  var input = address(); input.put("userId",2); input.put("customer",Map.of("id",2)); input.put("deleted",true);
  var result = mvc.perform(post("/api/addresses/me").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.isDefault").value(true)).andReturn();
  long id = json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
  assertEquals(1L,jdbc.queryForObject("SELECT user_id FROM delivery_address WHERE id=?",Long.class,id));
  mvc.perform(get("/api/addresses/me").param("userId","2")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
  input.put("address","更新后的9斋门口");
  mvc.perform(put("/api/addresses/"+id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.address").value("更新后的9斋门口"));
  mvc.perform(delete("/api/addresses/"+id)).andExpect(status().isOk());
  mvc.perform(get("/api/addresses/"+id)).andExpect(status().isNotFound());
  assertEquals(1,jdbc.queryForObject("SELECT is_deleted FROM delivery_address WHERE id=?",Integer.class,id));
 }

 @Test void switchingAndDeletingDefaultLeavesExactlyOneDefault() throws Exception {
  long first=createAddress(), second=createAddress();
  mvc.perform(put("/api/addresses/"+second+"/default")).andExpect(status().isOk());
  assertEquals(second,jdbc.queryForObject("SELECT id FROM delivery_address WHERE user_id=1 AND is_default=1 AND is_deleted=0",Long.class));
  mvc.perform(delete("/api/addresses/"+second)).andExpect(status().isOk());
  mvc.perform(get("/api/addresses/"+first)).andExpect(jsonPath("$.data.isDefault").value(true));
 }

 @ParameterizedTest @ValueSource(strings={"get","put","delete","default"})
 void cannotReadOrChangeAnotherPersonsAddress(String action) throws Exception {
  otherAddress();
  var request=switch(action) {
   case "put" -> put("/api/addresses/999").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(address()));
   case "delete" -> delete("/api/addresses/999");
   case "default" -> put("/api/addresses/999/default");
   default -> get("/api/addresses/999");
  };
  mvc.perform(request).andExpect(status().isNotFound());
  assertEquals("他人地址",jdbc.queryForObject("SELECT address FROM delivery_address WHERE id=999",String.class));
 }

 @ParameterizedTest @ValueSource(strings={"contactName","contactTel","address"})
 void requiredAddressFieldsAreValidatedOnServer(String field) throws Exception {
  var input=address(); input.put(field," ");
  mvc.perform(post("/api/addresses/me").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM delivery_address",Integer.class));
 }

 @Test void invalidPhoneSexAndOversizedAddressAreRejected() throws Exception {
  for (var entry: Map.of("contactTel","123","contactSex",99,"address","长".repeat(256)).entrySet()) {
   var input=address(); input.put(entry.getKey(),entry.getValue());
   mvc.perform(post("/api/addresses/me").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
  }
 }

 @Test void profileUpdateCannotChangeIdentityRolePasswordOrAvatarUrl() throws Exception {
  var input=profile(); input.put("id",2); input.put("username","hacked"); input.put("password","hacked");
  input.put("authorities",new String[]{"ADMIN"}); input.put("photo","https://untrusted.test/tracker.png");
  mvc.perform(put("/api/user").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.username").value("owner"))
   .andExpect(jsonPath("$.photo").value("/images/default-user-avatar.png")).andExpect(jsonPath("$.password").doesNotExist());
  mvc.perform(get("/api/user")).andExpect(jsonPath("$.phone").value("13900000003")).andExpect(jsonPath("$.firstName").value("李"));
  assertEquals("13900000002",jdbc.queryForObject("SELECT phone FROM person WHERE id=2",String.class));
  assertEquals("test-only",jdbc.queryForObject("SELECT password FROM users WHERE id=1",String.class));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM user_authority WHERE authority_name='ADMIN'",Integer.class));
 }

 @Test void duplicatePhoneCannotPartiallyUpdateProfile() throws Exception {
  var input=profile(); input.put("phone","13900000002");
  mvc.perform(put("/api/user").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input))).andExpect(status().isConflict());
  assertEquals("13900000001",jdbc.queryForObject("SELECT phone FROM person WHERE id=1",String.class));
 }

 @Test void optionalProfileFieldsCanBeCleared() throws Exception {
  var input=profile(); input.put("email",""); input.put("firstName",""); input.put("lastName","");
  mvc.perform(put("/api/user").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(""));
 }

 @Test void profileRejectsInvalidEmailAndPhone() throws Exception {
  for (var field : new String[]{"email","phone"}) {
   var input=profile(); input.put(field,"invalid");
   mvc.perform(put("/api/user").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
  }
 }

 @Test void favoriteIsIdempotentAndSharesLiveRatingWithSearch() throws Exception {
  String body="{\"merchantId\":1,\"userId\":2,\"collected\":true}";
  for(int i=0;i<2;i++) mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM merchant_interaction WHERE user_id=1 AND collected=1",Integer.class));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM merchant_interaction WHERE user_id=2",Integer.class));
  JsonNode search=json.readTree(mvc.perform(get("/api/businesses/search")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").get(0);
  JsonNode favorite=json.readTree(mvc.perform(get("/api/merchant/interaction/collections/me").param("userId","2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").get(0);
  assertEquals(search.get("score"),favorite.get("score"));
  assertEquals(4.5,favorite.get("score").asDouble());
  assertEquals(search.get("recommendationTags"),favorite.get("recommendationTags"));
  for(int i=0;i<2;i++) mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON).content("{\"merchantId\":1,\"collected\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/merchant/interaction/collections/me")).andExpect(jsonPath("$.data.length()").value(0));
 }

 @Test void cannotFavoriteUnapprovedStore() throws Exception {
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON).content("{\"merchantId\":2,\"collected\":true}")).andExpect(status().isNotFound());
 }

 @Test void hiddenStoreDisappearsButCanStillBeUnfavorited() throws Exception {
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON).content("{\"merchantId\":1,\"collected\":true}")).andExpect(status().isOk());
  jdbc.update("UPDATE business SET is_deleted=1 WHERE id=1");
  mvc.perform(get("/api/merchant/interaction/collections/me")).andExpect(jsonPath("$.data.length()").value(0));
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON).content("{\"merchantId\":1,\"collected\":false}")).andExpect(status().isOk());
 }

 @Test @WithMockUser(username="owner",authorities="RIDER")
 void riderSessionCannotUseCustomerAddressOrFavoriteEndpoints() throws Exception {
  mvc.perform(get("/api/addresses/me")).andExpect(status().isForbidden());
  mvc.perform(get("/api/merchant/interaction/collections/me")).andExpect(status().isForbidden());
 }

 @Test void partialInteractionUpdatePreservesTheOtherFlag() throws Exception {
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON)
   .content("{\"merchantId\":1,\"liked\":true,\"collected\":true}")).andExpect(status().isOk());
  // 收藏页只发送 collected；取消收藏不能同时取消点赞。
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON)
   .content("{\"merchantId\":1,\"collected\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/merchant/interaction/status/me").param("merchantId","1"))
   .andExpect(jsonPath("$.data.liked").value(true)).andExpect(jsonPath("$.data.collected").value(false));
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON)
   .content("{\"merchantId\":1,\"liked\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/merchant/interaction/status/me").param("merchantId","1"))
   .andExpect(jsonPath("$.data.liked").value(false)).andExpect(jsonPath("$.data.collected").value(false));
 }

 @Test void emptyInteractionUpdateCannotCreateARow() throws Exception {
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON)
   .content("{\"merchantId\":1}")).andExpect(status().isBadRequest());
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM merchant_interaction",Integer.class));
 }

 @Test void cannotLikeUnapprovedStore() throws Exception {
  mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON)
   .content("{\"merchantId\":2,\"liked\":true,\"collected\":false}")).andExpect(status().isNotFound());
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM merchant_interaction",Integer.class));
 }
}
