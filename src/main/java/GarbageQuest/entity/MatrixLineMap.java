package GarbageQuest.entity;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MatrixLineMap {
    Map<WayPoint, Double> distances;
}
