package com.tju.elm_bk.profile;

import com.tju.elm_bk.ElmBkApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Isolated local preview. Test classpath only: never packaged in the production JAR. */
public class ProfilePreviewApplication {
 public static void main(String[] args) {
  var app=new SpringApplication(ElmBkApplication.class);
  app.setAdditionalProfiles("auth-test");
  var context=app.run("--spring.datasource.url=jdbc:h2:mem:profile_preview;MODE=MySQL;DB_CLOSE_DELAY=-1",
   "--spring.datasource.driver-class-name=org.h2.Driver","--spring.datasource.username=sa","--spring.datasource.password=",
   "--spring.sql.init.schema-locations=classpath:auth-schema.sql,classpath:profile-schema.sql",
   "--app.upload.directory="+System.getProperty("java.io.tmpdir")+"/lcw-profile-preview-uploads",
   "--server.port="+System.getenv().getOrDefault("PROFILE_PREVIEW_PORT","8080"));
  var jdbc=context.getBean(JdbcTemplate.class);
  var password=context.getBean(PasswordEncoder.class).encode("Study2026!");
  String[] roles={"USER","BUSINESS","RIDER","ADMIN"};
  String[] names={"preview_user","preview_merchant","preview_rider","preview_admin"};
  for(int i=0;i<roles.length;i++){
   jdbc.update("INSERT INTO authority(name) VALUES (?)",roles[i]);
   jdbc.update("INSERT INTO users(id,username,password,activated,is_deleted) VALUES (?,?,?,1,0)",i+1,names[i],password);
   jdbc.update("INSERT INTO person(id,phone,photo) VALUES (?,?,?)",i+1,"1390000000"+i,"/images/default-user-avatar.png");
   jdbc.update("INSERT INTO user_authority(user_id,authority_name) VALUES (?,?)",i+1,roles[i]);
  }
  jdbc.update("INSERT INTO business(id,business_name,business_img,start_price,delivery_price,demo_rating,demo_sales_count,status,is_deleted) VALUES (1,'北洋测试食堂','/images/default-store.svg',20,3,1.36,500,1,0),(2,'湖畔早餐店','/images/default-store.svg',10,0,NULL,50,1,0)");
  jdbc.update("INSERT INTO review(business_id,rating,is_hidden) VALUES (1,5,0),(1,4,0),(1,1,1)");
  System.out.println("Local profile preview ready: preview_user / Study2026!");
 }
}
