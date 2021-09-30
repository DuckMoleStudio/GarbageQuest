package GarbageQuest.service;

import GarbageQuest.entity.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlgGreedyMatrixMapV01 {
    public static Result Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint,MatrixLineMap> matrix,
            int avgSpeed,
            int carCapacity){

        Result result = new Result();
        result.setMethodUsed("Simple Greedy Algorithm, matrix distance calculation, hashmap V.01");
        List<Itinerary> ii = new ArrayList<>();
        result.setItineraries(ii);

        WayPoint start = new WayPoint();
        WayPoint dump = new WayPoint();


        // get start & dump
        for(WayPoint ww: wayPoints)
        {
            switch (ww.getType())
            {
                case Garbage_Dump:
                    dump = ww;
                    break;

                case Base:
                    start = ww;
                    break;

                default:
                    break;
            }
        }
        wayPoints.remove(start);
        wayPoints.remove(dump);

        int itCount = 0;

        while (!wayPoints.isEmpty())
        {
            //timer
            double startTime = System.currentTimeMillis();

            Itinerary itinerary = new Itinerary(); // new car & new itinerary start here
            itinerary.setCar(new Car("Kamaz Othodov # "+(++itCount), WayPointType.Garbage_Site, carCapacity));

            List<WayPoint> ll = new ArrayList<>();
            ll.add(start);

            itinerary.setWayPointList(ll);
            itinerary.setTimeStart(LocalTime.parse("06:00"));
            int curLoad = 0;

            List<WayPoint> subWP = new ArrayList<>(wayPoints); // suitable for current itinerary
            WayPoint curWP = start;
            LocalTime curTime = LocalTime.parse("06:00");

            long minWait=-1; // reset
            long curWait;
            List<WayPoint> tmpWP = new ArrayList<>(); // for temporary non-available

            while (!subWP.isEmpty())
            {
                // get nearest operational
                // 1. get nearest

                MatrixElement me = Matrix.NearestMap(curWP, matrix, subWP);
                WayPoint tryWP = me.getWayPoint();
                double curDistance = me.getDistance();


                // 2. check temporal availability
                if(Duration.between(
                        curTime.plus(
                                Duration.ofSeconds((long)curDistance/avgSpeed)),
                        tryWP.getTimeClose()).getSeconds()>0)
                {
                    curWait = Duration.between
                            (curTime.plus(Duration.ofSeconds((long)curDistance/avgSpeed)),tryWP.getTimeOpen()).getSeconds();

                    if(curWait <= 0)
                    {
                        // timeslot ok, visit this WP
                        itinerary.getWayPointList().add(tryWP);
                        itinerary.setDistance(itinerary.getDistance()+curDistance);
                        curTime = curTime.plus(Duration.ofSeconds((long)curDistance/avgSpeed)); // enroute time
                        curTime = curTime.plus(tryWP.getDuration()); // loading time
                        curWP = tryWP;
                        wayPoints.remove(tryWP);
                        subWP.remove(tryWP);
                        subWP.addAll(tmpWP); // ones we were early for, try on next pass
                        tmpWP.clear();
                        minWait = -1;
                        // check if full
                        if(++curLoad == itinerary.getCar().getCapacity())
                        {
                            // visit dump site
                            itinerary.getWayPointList().add(dump);

                            curDistance = Matrix.DistanceBetweenMap(curWP,dump,matrix);

                            itinerary.setDistance(itinerary.getDistance()+curDistance);
                            curTime = curTime.plus(Duration.ofSeconds((long)curDistance/avgSpeed)); // enroute time
                            curTime = curTime.plus(dump.getDuration()); // unloading time
                            curWP = dump;
                            curLoad = 0;
                        }
                    }
                    else // we are early, remove for this pass but restore later
                    {
                        if((curWait < minWait) || (minWait < 0)) {minWait = curWait;}
                        tmpWP.add(tryWP);
                        subWP.remove(tryWP);
                        if(subWP.isEmpty()) // we are early everywhere!
                        {
                            curTime = curTime.plus(Duration.ofSeconds(minWait)); // wait for 1st to open
                            minWait = -1;
                            subWP.addAll(tmpWP); // ones we were early for, try on next pass
                            tmpWP.clear();
                        }
                    }
                }
                else{subWP.remove(tryWP);} // we are late for this WP, discard totally
            }
            // now complete itinerary
            itinerary.setTimeEnd(curTime);
            result.getItineraries().add(itinerary);
            result.setDistanceTotal(result.getDistanceTotal()+itinerary.getDistance());

            //System.out.println("Iteration " + itCount + " completed in "+ (System.currentTimeMillis()-startTime));
        }
        // now complete result
        result.setItineraryQty(itCount);

        return result;
    }
}
