package GarbageQuest.entity;

import lombok.*;

import java.util.List;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class MatrixStorageLine {
    WayPoint wayPoint;
    List<Double> distances;
}
