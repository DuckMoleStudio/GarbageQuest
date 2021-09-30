package GarbageQuest.mosData;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

public class XLSPaidParkings {
    long global_id;
    String admArea;
    String district;
    String address;
    String coordinates;
}
