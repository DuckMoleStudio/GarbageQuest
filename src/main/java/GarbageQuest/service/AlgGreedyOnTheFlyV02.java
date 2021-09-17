package GarbageQuest.service;

import GarbageQuest.entity.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.round;

public class AlgGreedyOnTheFlyV02 {
    public static Result Calculate(List<WayPoint> wayPoints, int avgSpeed, int carCapacity, int loadFactor,
                                   boolean dumpIfNear, boolean dumpIfWaiting, boolean waitIfNear)
    {

        Result result = new Result();
        result.setMethodUsed("Simple Greedy Algorithm, on the fly distance calculation, V.02");
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

            double minWaitDistance=0;
            WayPoint nearestClosedWP = new WayPoint();

            while (!subWP.isEmpty())
            {
                // get nearest operational
                // 1. get nearest

                WayPoint tryWP = CalcNearestSimple.calc(curWP, subWP);
                double curDistance = CalcDistanceSimple.Calc
                        (curWP.getLat(),curWP.getLon(),tryWP.getLat(),tryWP.getLon());


                // 2. check temporal availability
                if(Duration.between(curTime.plus(Duration.ofSeconds((long)
                        curDistance/avgSpeed)),tryWP.getTimeClose()).getSeconds()>0)
                {
                    curWait = Duration.between
                            (curTime.plus(Duration.ofSeconds((long)curDistance/avgSpeed)),tryWP.getTimeOpen()).getSeconds();

                    if(curWait <= 0)
                    {
                        // timeslot ok

                        // -- V02 -- visit nearest closed if it opens before we reach nearest open
                        if((curDistance/avgSpeed > minWaitDistance/avgSpeed+minWait) && minWait > 0 && waitIfNear)
                        {
                            tryWP = nearestClosedWP;
                            curTime = curTime.plus(Duration.ofSeconds(minWait)); // wait time
                            curDistance = minWaitDistance;
                            System.out.println("Car " + itCount + " switches to soon-opening");
                        }



                        if((curDistance > CalcDistanceSimple.Calc(dump.getLat(),dump.getLon(),
                            curWP.getLat(),curWP.getLon())) && dumpIfNear && curLoad > loadFactor) // -- V02 -- dump if near
                        {
                            // visit dump
                            System.out.println("Car " + itCount + " unloads when near with load " + curLoad);
                            itinerary.getWayPointList().add(dump);
                            curDistance = CalcDistanceSimple.Calc
                                    (curWP.getLat(), curWP.getLon(), dump.getLat(), dump.getLon());
                            itinerary.setDistance(itinerary.getDistance() + curDistance);
                            curTime = curTime.plus(Duration.ofSeconds((long) curDistance / avgSpeed)); // enroute time
                            curTime = curTime.plus(dump.getDuration()); // unloading time
                            curWP = dump;
                            curLoad = 0;

                            subWP.addAll(tmpWP); // ones we were early for, try on next pass
                            tmpWP.clear();
                            minWait = -1;
                        }

                        else
                        {
                            // visit this WP
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
                            if (++curLoad == itinerary.getCar().getCapacity()) {
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
                        }
                    }
                    else // we are early, remove for this pass but restore later
                    {
                        if((curWait < minWait) || (minWait < 0))
                        {
                            minWait = curWait;
                            minWaitDistance = curDistance;
                            nearestClosedWP = tryWP;
                        }
                        tmpWP.add(tryWP);
                        subWP.remove(tryWP);
                        if(subWP.isEmpty()) // we are early everywhere!
                        {
                            if(dumpIfWaiting && curLoad>loadFactor) // -- V02 --
                            {
                                System.out.println("Car " + itCount + " unloads while waiting with load " + curLoad);
                                itinerary.getWayPointList().add(dump);
                                curDistance=CalcDistanceSimple.Calc
                                        (curWP.getLat(),curWP.getLon(),dump.getLat(),dump.getLon());
                                itinerary.setDistance(itinerary.getDistance()+curDistance);
                                curTime = curTime.plus(Duration.ofSeconds((long)curDistance/avgSpeed)); // enroute time
                                curTime = curTime.plus(dump.getDuration()); // unloading time
                                curWP = dump;
                                curLoad = 0;
                                subWP.addAll(tmpWP); // ones we were early for, try on next pass
                                tmpWP.clear();
                                minWait = -1;
                            }
                            else
                            {
                                curTime = curTime.plus(Duration.ofSeconds(minWait)); // wait for 1st to open
                                minWait = -1;
                                subWP.addAll(tmpWP); // ones we were early for, try on next pass
                                tmpWP.clear();
                            }
                        }
                    }
                }
                else{subWP.remove(tryWP);} // we are late for this WP, discard totally
            }
            // now complete itinerary
            itinerary.setTimeEnd(curTime);
            result.getItineraries().add(itinerary);
            result.setDistanceTotal(result.getDistanceTotal()+itinerary.getDistance());
        }
        // now complete result
        result.setItineraryQty(itCount);

        return result;
    }
}
