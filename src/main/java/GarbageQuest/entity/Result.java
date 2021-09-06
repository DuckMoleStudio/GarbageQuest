package GarbageQuest.entity;

import lombok.*;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Result {

    String methodUsed;
    List<Itinerary> itineraries;
    double distanceTotal;
    int itineraryQty;

}
