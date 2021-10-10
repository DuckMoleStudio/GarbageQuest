package GarbageQuest.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

public class Car {

    String description;
    WayPointType type;
    int capacity;
}
