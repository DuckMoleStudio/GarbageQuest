package GarbageQuest.entity;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

public class DoubleMatrix {
    Map<WayPoint, MatrixLineMap> mapGood;
    Map<WayPoint, MatrixLineMap> mapBad;
    List<WayPoint> wayPointsGood;
    List<WayPoint> wayPointsBad;
}
