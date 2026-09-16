package com.tju.elm_bk.controller;

import com.tju.elm_bk.service.AmapService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/maps")
public class MapController {
    private final AmapService amap;
    public MapController(AmapService amap){this.amap=amap;}
    @GetMapping("/config") public Object config(){return amap.config();}
    @GetMapping("/search") public Object search(@RequestParam String keywords, @RequestParam(defaultValue="") String city){
        if(keywords.isBlank() || keywords.length()>100 || city.length()>50) throw new IllegalArgumentException("请输入有效地点名称");
        return amap.query("/v3/place/text", Map.of("keywords",keywords,"city",city,"offset","10","page","1","extensions","base"));
    }
    @GetMapping("/reverse") public Object reverse(@RequestParam String location){
        coordinate(location); return amap.query("/v3/geocode/regeo",Map.of("location",location,"extensions","base"));
    }
    @GetMapping("/convert") public Object convert(@RequestParam String location){
        coordinate(location); return amap.query("/v3/assistant/coordinate/convert",Map.of("locations",location,"coordsys","gps"));
    }
    @GetMapping("/route") public Object route(@RequestParam String origin,@RequestParam String destination){
        coordinate(origin); coordinate(destination);
        return amap.query("/v5/direction/bicycling",Map.of("origin",origin,"destination",destination,"show_fields","cost"));
    }
    @GetMapping("/_AMapService/**") public ResponseEntity<String> sdk(HttpServletRequest request,@RequestParam Map<String,String> params){
        String prefix="/api/maps/_AMapService/";
        String result=amap.sdk(request.getRequestURI().substring(prefix.length()),params);
        return ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Type", params.containsKey("callback")?"application/javascript;charset=UTF-8":"application/json;charset=UTF-8").body(result);
    }
    static void coordinate(String s){
        if(s==null || !s.matches("-?\\d{1,3}(\\.\\d{1,8})?,-?\\d{1,2}(\\.\\d{1,8})?")) throw new IllegalArgumentException("坐标格式无效");
        String[] p=s.split(","); if(Math.abs(Double.parseDouble(p[0]))>180 || Math.abs(Double.parseDouble(p[1]))>90) throw new IllegalArgumentException("坐标超出范围");
    }
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
    ResponseEntity<?> failure(RuntimeException e){return ResponseEntity.status(e instanceof IllegalArgumentException?400:503).body(Map.of("message",e.getMessage()));}
}
