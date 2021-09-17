import GarbageQuest.entity.Itinerary;
import GarbageQuest.entity.Result;
import GarbageQuest.entity.WayPoint;
import GarbageQuest.service.*;
import GarbageQuest.supplimentary.MockInput;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.round;

public class Application {
    public static void main(String[] args)
    {
        List<WayPoint> wayPointsCommon = MockInput.randomWP();
       /*
        for(WayPoint wp : wayPointsCommon)
        {
            System.out.println(wp);
        }
        */

        List<WayPoint> wayPoints = new ArrayList<>(wayPointsCommon);
        double startTime = System.currentTimeMillis();
        Result rr = AlgGreedyOnTheFlyV01.Calculate(wayPoints, 10, 25);
        double elapsedTime = System.currentTimeMillis() - startTime;
        double distCallCount= CalcDistanceSimple.CALL_COUNT;

/*
        for(Itinerary ii : rr.getItineraries())
        {
            System.out.println("\n\n"+ii.getCar().getDescription());
            System.out.println(ii.getTimeStart()+" - " + ii.getTimeEnd());
            System.out.println(ii.getDistance());
            for(WayPoint ww : ii.getWayPointList())
            {
                System.out.print(ww.getDescription()+" -> ");
            }
        }
 */


        for(Itinerary ii : rr.getItineraries())
        {
            System.out.println("\n\n"+ii.getCar().getDescription());
            System.out.println("No. of waypoints: " + ii.getWayPointList().size());

            System.out.println("Route distance: " + round(ii.getDistance())/1000);
            System.out.println("Avg hop: " + round(ii.getDistance()/(ii.getWayPointList().size())));
        }



        System.out.println("\n\nTotal distance: " + round(rr.getDistanceTotal())/1000 + " km");
        System.out.println("Cars assigned: " + rr.getItineraryQty());
        System.out.println("Calculated in: " + elapsedTime + " ms");
        System.out.println("by: " + rr.getMethodUsed());
        System.out.println("distance calculated: " + distCallCount/1000000 + " million times\n\n");


        wayPoints = new ArrayList<>(wayPointsCommon);
        startTime = System.currentTimeMillis();
        rr = AlgGreedyOnTheFlyV02.Calculate(wayPoints, 10, 25, 10, true, true, true);
        elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("\n\nTotal distance: " + round(rr.getDistanceTotal())/1000 + " km");
        System.out.println("Cars assigned: " + rr.getItineraryQty());
        System.out.println("Calculated in: " + elapsedTime + " ms");
        System.out.println("by: " + rr.getMethodUsed());
        distCallCount = CalcDistanceSimple.CALL_COUNT - distCallCount;
        System.out.println("distance calculated: " + distCallCount/1000000 + " million times\n\n");

        wayPoints = new ArrayList<>(wayPointsCommon);
        startTime = System.currentTimeMillis();
        rr = AlgGreedyOnTheFlyWOpt.Calculate(wayPoints, 10, 25, LocalTime.parse("06:00"), 0, 30);
        elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("\n\nTotal distance: " + round(rr.getDistanceTotal())/1000 + " km");
        System.out.println("Cars assigned: " + rr.getItineraryQty());
        System.out.println("Calculated in: " + elapsedTime + " ms");
        System.out.println("by: " + rr.getMethodUsed());
        distCallCount = CalcDistanceSimple.CALL_COUNT - distCallCount;
        System.out.println("distance calculated: " + distCallCount/1000000 + " million times");

    }
}
