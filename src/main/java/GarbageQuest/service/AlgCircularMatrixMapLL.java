package GarbageQuest.service;

import GarbageQuest.entity.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import static java.lang.Math.round;

public class AlgCircularMatrixMapLL {
    public static Result Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint,MatrixLineMap> matrix,
            int maxTime)
    {

        Result result = new Result();
        result.setMethodUsed("Circular route Algorithm, Long Lance");
        List<Itinerary> ii = new ArrayList<>();
        result.setItineraries(ii);

        WayPoint start = new WayPoint();


        // get start
        for(WayPoint ww: wayPoints)
        {
            switch (ww.getType())
            {


                case Base:
                    start = ww;
                    break;

                default:
                    break;
            }
        }
        wayPoints.remove(start);

        int itCount = 0;

        while (!wayPoints.isEmpty())
        {
            if (wayPoints.size() > 1)
            {
                //timer
                double startTime = System.currentTimeMillis();

                Itinerary itinerary = new Itinerary(); // new car & new itinerary start here
                itinerary.setCar(new Car("Lovely Rita # " + (++itCount), WayPointType.Paid_Parking, 0));

                TreeMap<Double, Hop> hopMap = new TreeMap<>();
                List<WayPoint> ll = new ArrayList<>();
                ll.add(start);

                // set first/last & second point
                MatrixElement me = Matrix.NearestMap(start, matrix, wayPoints);
                WayPoint curWP = me.getWayPoint();
                ll.add(curWP); // first

                double longDistance = 0;
                WayPoint longWP = Matrix.NearestMap(curWP,matrix,wayPoints).getWayPoint();
                for(WayPoint wp: wayPoints)
                {
                    double curDistance = Matrix.DistanceBetweenMap(curWP,wp,matrix) +
                            Matrix.DistanceBetweenMap(wp,curWP,matrix);
                    if((curDistance > longDistance)&&(curDistance<maxTime))
                    {
                        longDistance=curDistance;
                        longWP=wp;
                    }
                }

                ll.add(longWP); // second
                ll.add(curWP); // last


                hopMap.put(Matrix.DistanceBetweenMap(curWP,longWP,matrix), new Hop(curWP, longWP));
                hopMap.put(Matrix.DistanceBetweenMap(longWP,curWP,matrix), new Hop(longWP, curWP));

                double totalDistance = Matrix.DistanceBetweenMap(curWP,longWP,matrix) +
                        Matrix.DistanceBetweenMap(longWP,curWP,matrix);

                wayPoints.remove(curWP);
                wayPoints.remove(longWP);

                itinerary.setWayPointList(ll);

                List<WayPoint> subWP = new ArrayList<>(wayPoints); // suitable for current itinerary

                // MAIN CIRCULATION

                while (!subWP.isEmpty() && !hopMap.isEmpty()) {
                    // get longest hop
                    double oldDistance = hopMap.lastKey();
                    Hop oldHop = hopMap.get(oldDistance);
                    hopMap.remove(oldDistance);


                    // get matrix line sorted by distance
                    MatrixLineMap mlFrom = matrix.get(oldHop.getFrom());
                    TreeMap<Double, WayPoint> distancesFrom = new TreeMap<>();
                    for(Map.Entry<WayPoint,Double> tempME: mlFrom.getDistances().entrySet())
                        distancesFrom.put(tempME.getValue(),tempME.getKey());

                    // try to route thru nearest possible
                    boolean found = false;
                    while (!distancesFrom.isEmpty()&&!found)
                    {
                        WayPoint tryWP = distancesFrom.get(distancesFrom.firstKey());
                        if(wayPoints.contains(tryWP))
                        {
                            double newDistance = distancesFrom.firstKey() + Matrix.DistanceBetweenMap(
                                    tryWP,
                                    oldHop.getTo(),
                                    matrix);
                            if((newDistance + totalDistance - oldDistance) < maxTime)
                            {
                                //new 2 hops
                                hopMap.put(distancesFrom.firstKey(),
                                        new Hop(oldHop.getFrom(), tryWP));

                                hopMap.put(Matrix.DistanceBetweenMap(tryWP, oldHop.getTo(), matrix),
                                        new Hop(tryWP, oldHop.getTo()));

                                totalDistance += newDistance;
                                totalDistance -= oldDistance;


                                int index = itinerary.getWayPointList().indexOf(oldHop.getFrom()); // add to itinerary
                                itinerary.getWayPointList().add(index + 1, tryWP);
                                subWP.remove(tryWP); // remove from lists
                                wayPoints.remove(tryWP);
                                found = true;
                            }
                        }
                        distancesFrom.remove(distancesFrom.firstKey());
                    }
                }
                // now complete itinerary
                itinerary.setDistance(totalDistance);
                result.getItineraries().add(itinerary);
                result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());

                System.out.println("Iteration " + itCount + " completed in " + (System.currentTimeMillis() - startTime));
            }
            else
            {
                WayPoint lastWP = wayPoints.get(0);
                System.out.println("Only 1 wp left, " + lastWP.getDescription() + ", no route for it");
                wayPoints.remove(lastWP);
            }

        }
        // now complete result
        result.setItineraryQty(itCount);

        return result;
    }
}
