package GarbageQuest.service;

import GarbageQuest.entity.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AlgGreedyOnTheFlyWOpt {
    public static Result Calculate(List<WayPoint> wayPointList,
                                   int avgSpeed,
                                   int carCapacity,
                                   LocalTime timeStart,
                                   int optMark,
                                   int optIterations){

        Result result = new Result();
        result.setMethodUsed("Simple Greedy Algorithm with Partial Optimization");
        List<Itinerary> ii = new ArrayList<>();
        result.setItineraries(ii);

        WayPoint start = new WayPoint();
        WayPoint dump = new WayPoint();

        // get start & dump
        for(WayPoint ww: wayPointList)
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
        wayPointList.remove(start);
        wayPointList.remove(dump);

        int itCount = 0;

        while (!wayPointList.isEmpty())
        {

            List<WayPoint> subWayPointList = new ArrayList<>(wayPointList); // suitable for current itinerary
            Itinerary itinerary = new Itinerary();

            if (itCount < optMark)
            {
                // no additional optimization yet
                itinerary = fillItinerary(
                        start,
                        dump,
                        wayPointList,
                        subWayPointList,
                        timeStart,
                        avgSpeed,
                        carCapacity);
            }
            else
            {
                // now create several variants for comparison & optimization
                List<WayPoint> altWayPointList = new ArrayList<>(wayPointList);
                List<WayPoint> optWayPointList = new ArrayList<>(subWayPointList);

                Itinerary refItinerary = fillItinerary(
                        start,
                        dump,
                        altWayPointList,
                        optWayPointList,
                        timeStart,
                        avgSpeed,
                        carCapacity); // reference standard route

                Itinerary minItinerary = refItinerary;
                List<WayPoint> minWayPointList = new ArrayList<>(altWayPointList);;

                for(int i=0; i<optIterations; i++)
                {
                    optWayPointList = new ArrayList<>(subWayPointList);
                    if(refItinerary.getWayPointList().size()>(i+1) && optWayPointList.size()>2)
                    {
                        WayPoint ignoredWP = refItinerary.getWayPointList().get(i + 1);
                        optWayPointList.remove(ignoredWP);
                        altWayPointList = new ArrayList<>(wayPointList);

                        Itinerary curItinerary = fillItinerary(
                                start,
                                dump,
                                altWayPointList,
                                optWayPointList,
                                timeStart,
                                avgSpeed,
                                carCapacity); // with wp changed 1 by 1
                        if (curItinerary.getDistance() < minItinerary.getDistance())
                        {
                            System.out.println("Optimized for "
                                    + itCount
                                    + "("
                                    + i
                                    +")"
                                    + " : "
                                    + minItinerary.getDistance()
                                    + " - "
                                    + curItinerary.getDistance());
                            minItinerary = curItinerary;
                            minWayPointList = altWayPointList;

                        }
                    }

                }
                itinerary=minItinerary;
                wayPointList = minWayPointList;
            }

                // now complete itinerary
            itinerary.setCar(new Car("Kamaz Othodov # "+(++itCount), WayPointType.Garbage_Site, carCapacity));
                result.getItineraries().add(itinerary);
                result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());


        }
        // now complete result
        result.setItineraryQty(itCount);

        return result;
    }

    private static Itinerary fillItinerary(WayPoint start,
                                    WayPoint dump,
                                    List<WayPoint> allWPList,
                                    List<WayPoint> subWPList,
                                    LocalTime startTime,
                                    int avgSpeed,
                                    int capacity)
    {
        Itinerary itinerary = new Itinerary();
        List<WayPoint> ll = new ArrayList<>();
        ll.add(start);
        itinerary.setWayPointList(ll);
        itinerary.setTimeStart(startTime);

        List<WayPoint> tmpWPList = new ArrayList<>();
        WayPoint curWP=start;
        LocalTime curTime = startTime;
        long minWait=-1; // reset
        long curWait;
        int curLoad=0;

        while (!subWPList.isEmpty())
        {
            // get nearest operational
            // 1. get nearest

            WayPoint tryWP = CalcNearestSimple.calc(curWP, subWPList);
            double curDistance = CalcDistanceSimple.Calc
                    (curWP.getLat(), curWP.getLon(), tryWP.getLat(), tryWP.getLon());

            // 2. check temporal availability
            if (Duration.between(curTime.plus(Duration.ofSeconds((long)
                    curDistance / avgSpeed)), tryWP.getTimeClose()).getSeconds() > 0)
            {
                curWait = Duration.between
                        (curTime.plus(Duration.ofSeconds((long) curDistance / avgSpeed)), tryWP.getTimeOpen()).getSeconds();

                if (curWait <= 0) {
                    // timeslot ok, visit this WP
                    itinerary.getWayPointList().add(tryWP);
                    itinerary.setDistance(itinerary.getDistance() + curDistance);
                    curTime = curTime.plus(Duration.ofSeconds((long) curDistance / avgSpeed)); // enroute time
                    curTime = curTime.plus(tryWP.getDuration()); // loading time
                    curWP = tryWP;
                    allWPList.remove(tryWP);
                    subWPList.remove(tryWP);
                    subWPList.addAll(tmpWPList); // ones we were early for, try on next pass
                    tmpWPList.clear();
                    minWait = -1;
                    // check if full
                    if (++curLoad == capacity) {
                        // visit dump site
                        itinerary.getWayPointList().add(dump);
                        curDistance = CalcDistanceSimple.Calc
                                (curWP.getLat(), curWP.getLon(), dump.getLat(), dump.getLon());
                        itinerary.setDistance(itinerary.getDistance() + curDistance);
                        curTime = curTime.plus(Duration.ofSeconds((long) curDistance / avgSpeed)); // enroute time
                        curTime = curTime.plus(dump.getDuration()); // unloading time
                        curWP = dump;
                        curLoad = 0;
                    }
                } else // we are early, remove for this pass but restore later
                {
                    if ((curWait < minWait) || (minWait < 0)) {
                        minWait = curWait;
                    }
                    ;
                    tmpWPList.add(tryWP);
                    subWPList.remove(tryWP);
                    if (subWPList.isEmpty()) // we are early everywhere!
                    {
                        curTime = curTime.plus(Duration.ofSeconds(minWait)); // wait for 1st to open
                        minWait = -1;
                        subWPList.addAll(tmpWPList); // ones we were early for, try on next pass
                        tmpWPList.clear();
                    }
                }
            } else
            {
                subWPList.remove(tryWP);
            } // we are late for this WP, discard totally
        }
        itinerary.setTimeEnd(curTime);

        return itinerary;
    }

}
