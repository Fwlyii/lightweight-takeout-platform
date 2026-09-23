package elm_bk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Fixed upstream and bounded requests: never an arbitrary URL/credential proxy. */
@Service
public class AmapService {
    private final ObjectMapper json;
    private final String webKey, jsKey, securityCode;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String, Cached> cache = new LinkedHashMap<>();
    private long minute = 0, day = 0;
    private int minuteCalls = 0, dayCalls = 0;
    private record Cached(long time, JsonNode data) {}

    public AmapService(ObjectMapper json, @Value("${AMAP_WEB_KEY:}") String webKey,
            @Value("${AMAP_JS_KEY:}") String jsKey, @Value("${AMAP_SECURITY_CODE:}") String securityCode) {
        this.json=json; this.webKey=webKey; this.jsKey=jsKey; this.securityCode=securityCode;
    }
    public Map<String,Object> config() {
        return Map.of("enabled", !webKey.isBlank() && !jsKey.isBlank() && !securityCode.isBlank(),
                "jsKey", jsKey, "coordinateSystem", "GCJ-02");
    }
    public synchronized JsonNode query(String path, Map<String,String> params) {
        if (webKey.isBlank()) throw new IllegalStateException("地图服务尚未配置");
        String cacheKey=path+new TreeMap<>(params);
        long now=System.currentTimeMillis();
        Cached hit=cache.get(cacheKey);
        if(hit!=null && now-hit.time<600_000) return hit.data;
        budget(now);
        var values=new LinkedHashMap<>(params); values.put("key",webKey); values.put("output","JSON");
        String body=fetch("https://restapi.amap.com"+path, values);
        try {
            JsonNode data=json.readTree(body);
            if (!"1".equals(data.path("status").asText())) throw new IllegalStateException("地图服务暂不可用，请稍后重试（"+data.path("infocode").asText("unknown")+"）");
            if(cache.size()>=256) cache.remove(cache.keySet().iterator().next());
            cache.put(cacheKey,new Cached(now,data));
            return data;
        } catch (java.io.IOException e) { throw new IllegalStateException("地图返回异常，请重试"); }
    }
    private void budget(long now) {
        if(minute!=now/60_000){ minute=now/60_000; minuteCalls=0; }
        if(day!=now/86_400_000){ day=now/86_400_000; dayCalls=0; }
        if(minuteCalls>=90 || dayCalls>=1000) throw new IllegalStateException("地图请求较多，请稍后重试");
        minuteCalls++; dayCalls++;
    }
    public synchronized String sdk(String path, Map<String,String> params) {
        if(securityCode.isBlank() || jsKey.isBlank()) throw new IllegalStateException("地图服务尚未配置");
        if(!Set.of("v4/map/styles", "v3/geocode/regeo", "v3/assistant/coordinate/convert").contains(path)) throw new IllegalArgumentException("不支持的地图请求");
        var values=new LinkedHashMap<String,String>();
        for(var entry:params.entrySet()) {
            if(!Set.of("key","jscode","callback").contains(entry.getKey()) && entry.getKey().matches("[a-zA-Z_]{1,40}") && entry.getValue().length()<1024) values.put(entry.getKey(),entry.getValue());
        }
        String callback=params.get("callback");
        if(callback!=null) {
            if(!callback.matches("[a-zA-Z_$][a-zA-Z0-9_$.]{0,100}")) throw new IllegalArgumentException("回调无效");
            values.put("callback",callback);
        }
        budget(System.currentTimeMillis()); values.put("key",jsKey); values.put("jscode",securityCode);
        return fetch((path.startsWith("v4/map/")?"https://webapi.amap.com/":"https://restapi.amap.com/")+path,values);
    }
    private String fetch(String base, Map<String,String> params) {
        String query=params.entrySet().stream().map(e->encode(e.getKey())+"="+encode(e.getValue())).collect(java.util.stream.Collectors.joining("&"));
        try {
            var response=client.send(HttpRequest.newBuilder(URI.create(base+"?"+query)).timeout(Duration.ofSeconds(8)).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200 || response.body().length()>2_000_000) throw new IllegalStateException("地图服务暂不可用");
            return response.body();
        } catch (InterruptedException e) {Thread.currentThread().interrupt(); throw new IllegalStateException("地图请求已中断");}
        catch (java.io.IOException e) {throw new IllegalStateException("地图连接超时，请重试或搜索选址");}
    }
    private static String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
