package GarbageQuest.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

public class ItineraryCirc {


    List<WayPoint> wayPointList;

    double distance;
}
