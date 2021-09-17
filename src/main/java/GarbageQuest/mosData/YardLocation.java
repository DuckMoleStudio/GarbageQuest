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
public class YardLocation {


    @Setter(AccessLevel.NONE)
    String UNOM;

    String District;
    String AdmArea;
    String Address;

    @JsonProperty("UNOM")
    public void setUNOM(String UNOM) {
        this.UNOM = UNOM;
    }
}
