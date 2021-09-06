package GarbageQuest.entity;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Itinerary {

    Car car;
    List<WayPoint> wayPointList;
    LocalTime timeStart, timeEnd;
    double distance;
}
