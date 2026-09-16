package com.tju.elm_bk.service;
import com.tju.elm_bk.entity.GeoPoint;
import com.tju.elm_bk.adapter.impl.AmapUriMapAdapter;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class GeoPointTest {
 @Test void openingAShopRequiresMapCoordinatesAndAddress(){var p=new GeoPoint();assertThrows(RuntimeException.class,p::requireCoordinates);p=point("117","39");assertThrows(RuntimeException.class,p::requireCoordinates);p.setFormattedAddress("高德选中地点");p.requireCoordinates();}
 private GeoPoint point(String lon,String lat){var p=new GeoPoint();p.setLongitude(new BigDecimal(lon));p.setLatitude(new BigDecimal(lat));return p;}
 @Test void validatesPairsAndBounds(){var p=point("117","39");p.validateCoordinates();p.setLatitude(null);assertThrows(RuntimeException.class,p::validateCoordinates);p=point("181","39");assertThrows(RuntimeException.class,p::validateCoordinates);}
 @Test void unknownNeverBecomesInventedDistance(){assertNull(GeoPoint.distanceKm(new GeoPoint(),point("117","39")));assertEquals(0,GeoPoint.distanceKm(point("117","39"),point("117","39")).compareTo(BigDecimal.ZERO));assertTrue(GeoPoint.distanceKm(point("117","39"),point("117.1","39.1")).doubleValue()>10);}
 @Test void navigationUsesGcjCoordinatesAndEncodesAddress(){var url=new AmapUriMapAdapter().navigationUrl("测试店 & 安全",point("117.3","39"));assertTrue(url.startsWith("https://uri.amap.com/navigation?"));assertTrue(url.contains("117.3,39,"));assertTrue(url.contains("coordinate=gaode"));assertTrue(url.contains("%26"));}
}
