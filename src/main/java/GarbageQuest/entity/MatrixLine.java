package GarbageQuest.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MatrixLine {
    WayPoint wayPoint;
    List<MatrixElement> distances;
}
