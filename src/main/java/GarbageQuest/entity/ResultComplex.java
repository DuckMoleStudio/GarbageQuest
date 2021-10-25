package GarbageQuest.entity;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

public class ResultComplex {

    String methodUsed;
    Map<Car, List<ItineraryCirc>> itineraries;
    List<Car> cars;
    double distanceTotal;
    int itineraryQty;

}
