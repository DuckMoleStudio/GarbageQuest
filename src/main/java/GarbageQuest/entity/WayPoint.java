package GarbageQuest.entity;

import lombok.*;

import java.time.Duration;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class WayPoint {

    double lat, lon;
    String description;
    LocalTime timeOpen, timeClose;
    Duration duration;
    WayPointType type;
    int capacity;
}
