package elm_bk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import elm_bk.exception.APIException;
import lombok.Data;
import java.math.BigDecimal;

/** Persisted coordinates are always GCJ-02 (AMap), never raw browser WGS-84. */
@Data
public class GeoPoint {
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String poiId;
    private String adcode;
    private String formattedAddress;

    @JsonIgnore
    public boolean hasCoordinates() { return longitude != null && latitude != null; }

    public void validateCoordinates() {
        if ((longitude == null) != (latitude == null)
                || longitude != null && longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0
                || latitude != null && latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || poiId != null && poiId.length() > 80 || adcode != null && !adcode.matches("[0-9]{6}")
                || formattedAddress != null && formattedAddress.length() > 255) {
            throw new APIException("地图位置无效，请重新选点");
        }
    }

    public void requireCoordinates() {
        validateCoordinates();
        if (!hasCoordinates() || formattedAddress == null || formattedAddress.isBlank()) {
            throw new APIException("开店前请在高德地图选择并确认店铺位置");
        }
    }

    public void copyLocationTo(GeoPoint target) {
        validateCoordinates();
        target.setLongitude(longitude); target.setLatitude(latitude);
        target.setPoiId(poiId); target.setAdcode(adcode); target.setFormattedAddress(formattedAddress);
    }

    public static BigDecimal distanceKm(GeoPoint from, GeoPoint to) {
        if (from == null || to == null || !from.hasCoordinates() || !to.hasCoordinates()) return null;
        double a = Math.toRadians(from.latitude.doubleValue()), b = Math.toRadians(to.latitude.doubleValue());
        double d = Math.toRadians(to.longitude.subtract(from.longitude).doubleValue());
        double h = Math.pow(Math.sin((b-a)/2), 2) + Math.cos(a)*Math.cos(b)*Math.pow(Math.sin(d/2),2);
        return BigDecimal.valueOf(6371.0088 * 2 * Math.asin(Math.sqrt(Math.min(1, h))))
                .setScale(1, java.math.RoundingMode.HALF_UP);
    }
}
