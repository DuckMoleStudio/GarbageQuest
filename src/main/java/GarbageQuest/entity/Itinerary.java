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
@EqualsAndHashCode

public class Itinerary {

    Car car;
    List<WayPoint> wayPointList;
    List<LocalTime> arrivals;
    LocalTime timeStart, timeEnd;
    double distance;
}
