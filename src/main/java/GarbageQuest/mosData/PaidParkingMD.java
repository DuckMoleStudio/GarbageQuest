package GarbageQuest.mosData;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
public class PaidParkingMD {

    String ParkingName;

    @Setter(AccessLevel.NONE)
    long global_id;

    String AdmArea;
    String District;
    String Address;
    int CarCapacity;
    int CarCapacityDisabled;


    @Setter(AccessLevel.NONE)
    geoData geoData;

    @JsonProperty("geoData")
    public void setGeoData(GarbageQuest.mosData.geoData geoData) {
        this.geoData = geoData;
    }

    @JsonProperty("global_id")
    public void setGlobal_id(long global_id) {
        this.global_id = global_id;
    }
}
