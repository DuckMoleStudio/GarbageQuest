package GarbageQuest.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MatrixElement {
    WayPoint wayPoint;
    double distance;
}
