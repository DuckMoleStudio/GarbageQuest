package GarbageQuest.service;

import GarbageQuest.entity.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlgGreedySimpleV01 {
    public static Result Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint,MatrixLineMap> matrix,
            int noOfRoutes){

        Result result = new Result();
        result.setMethodUsed("Simple Greedy Algorithm, no timeslots V.01");
        List<Itinerary> ii = new ArrayList<>();
        result.setItineraries(ii);

        WayPoint start = new WayPoint();

        // get start
        for(WayPoint ww: wayPoints)
        {
            if (ww.getType() == WayPointType.Base) {
                start = ww;
            }
        }
        wayPoints.remove(start);


        int itCount = 0;
        int maxPoints = wayPoints.size()/noOfRoutes+1;



        while(!wayPoints.isEmpty())
        {
            Itinerary itinerary = new Itinerary(); // new car & new itinerary start here
            itinerary.setCar(new Car("Rita # " + (++itCount), WayPointType.Paid_Parking, 0));
            WayPoint curWP = start;
            List<WayPoint> ll = new ArrayList<>();
            ll.add(curWP);
            itinerary.setWayPointList(ll);


            itinerary.setTimeStart(LocalTime.parse("06:00"));
            LocalTime curTime = LocalTime.parse("06:00");

            List<LocalTime> tt = new ArrayList<>();
            tt.add(curTime);
            itinerary.setArrivals(tt);

            int wpCount = 0;
            while (!wayPoints.isEmpty()&&((wpCount++)<=maxPoints))

            {
                MatrixElement me = Matrix.NearestMap(curWP, matrix, wayPoints);
                WayPoint tryWP = me.getWayPoint();
                double curDistance = me.getDistance();

                // visit this WP
                itinerary.getWayPointList().add(tryWP);
                itinerary.setDistance(itinerary.getDistance() + curDistance);
                //curTime = curTime.plus(Duration.ofSeconds((long) curDistance / avgSpeed)); // enroute time
                itinerary.getArrivals().add(curTime);
                //curTime = curTime.plus(tryWP.getDuration()); // loading time
                curWP = tryWP;
                wayPoints.remove(tryWP);
            }
            // now complete itinerary

            // return to base
            itinerary.getWayPointList().add(start);
            itinerary.setDistance(itinerary.getDistance() + Matrix.DistanceBetweenMap(curWP,start,matrix));

            itinerary.setTimeEnd(curTime);
            result.getItineraries().add(itinerary);
            result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());

            //System.out.println("Iteration " + itCount + " completed in "+ (System.currentTimeMillis()-startTime));
        }
        // now complete result
        result.setItineraryQty(itCount);

        return result;
    }
}
