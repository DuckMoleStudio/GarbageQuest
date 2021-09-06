package GarbageQuest.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class Car {

    String description;
    WayPointType type;
    int capacity;
}
