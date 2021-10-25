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

public class Result {

    String methodUsed;
    List<Itinerary> itineraries;
    double distanceTotal;
    int itineraryQty;

}
