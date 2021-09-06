import GarbageQuest.entity.Itinerary;
import GarbageQuest.entity.Result;
import GarbageQuest.entity.WayPoint;
import GarbageQuest.service.CalcSimple;
import GarbageQuest.supplimentary.MockInput;

import java.util.List;

public class Application {
    public static void main(String[] args)
    {
        List<WayPoint> wayPoints = MockInput.randomWP();
        for(WayPoint wp : wayPoints)
        {
            System.out.println(wp);
        }

        Result rr = CalcSimple.Calculate(wayPoints);

        for(Itinerary ii : rr.getItineraries())
        {
            System.out.println("\n"+ii.getCar().getDescription());
            System.out.print(ii.getTimeStart()+" - ");
            System.out.println(ii.getTimeEnd());
            System.out.println(ii.getDistance());
            for(WayPoint ww : ii.getWayPointList())
            {
                System.out.print(ww.getDescription()+" -> ");
            }
        }

        System.out.println("\n\n" + rr.getDistanceTotal());
        System.out.println(rr.getItineraryQty());

    }
}
