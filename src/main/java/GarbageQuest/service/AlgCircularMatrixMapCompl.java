package GarbageQuest.service;

import GarbageQuest.entity.*;

import java.util.*;

import static java.lang.Math.round;

public class AlgCircularMatrixMapCompl {
    public static ResultComplex Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint,MatrixLineMap> matrix,
            int maxTime, int noOfCars)
    {

        ResultComplex result = new ResultComplex();
        result.setMethodUsed("Circular route Algorithm, Complex routes");
        result.setItineraries(new HashMap<>());
        List<Car> cars = new ArrayList<>();
        Map<Car,WayPoint> newStart = new HashMap<>();

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

        for(int i=0;i<noOfCars;i++)
        {
            // create cars & itineraries
            Car car = new Car("Lovely Rita # " + (i+1), WayPointType.Paid_Parking, 0);
            cars.add(car);
            result.getItineraries().put(car, new ArrayList<>());
            newStart.put(car,start);
        }

        int itCount = 0;

        while (!wayPoints.isEmpty())
        {

                for (Car car: cars)
                {
                    if (wayPoints.size() > 1)
                    {
                    //timer
                    double startTime = System.currentTimeMillis();
                    itCount++;
                    ItineraryCirc itinerary = new ItineraryCirc();
                    WayPoint curStart = newStart.get(car);

                    TreeMap<Double, Hop> hopMap = new TreeMap<>();
                    List<WayPoint> ll = new ArrayList<>();
                    ll.add(curStart);

                    // set first/last & second point
                    MatrixElement me = Matrix.NearestMap(curStart, matrix, wayPoints);
                    ll.add(me.getWayPoint()); // first
                    WayPoint curWP = me.getWayPoint();


                    me = Matrix.NearestMap(curWP, matrix, wayPoints);
                    ll.add(me.getWayPoint()); // second
                    ll.add(curWP); // last


                    hopMap.put(me.getDistance(), new Hop(curWP, me.getWayPoint()));
                    hopMap.put(Matrix.DistanceBetweenMap(me.getWayPoint(), curWP, matrix),
                            new Hop(me.getWayPoint(), curWP));

                    double totalDistance = me.getDistance() + Matrix.DistanceBetweenMap(me.getWayPoint(), curWP, matrix);

                    wayPoints.remove(curWP);
                    wayPoints.remove(me.getWayPoint());

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
                        for (Map.Entry<WayPoint, Double> tempME : mlFrom.getDistances().entrySet())
                            distancesFrom.put(tempME.getValue(), tempME.getKey());

                        // try to route thru nearest possible
                        boolean found = false;
                        while (!distancesFrom.isEmpty() && !found) {
                            WayPoint tryWP = distancesFrom.get(distancesFrom.firstKey());
                            if (wayPoints.contains(tryWP)) {
                                double newDistance = distancesFrom.firstKey() + Matrix.DistanceBetweenMap(
                                        tryWP,
                                        oldHop.getTo(),
                                        matrix);
                                if ((newDistance + totalDistance - oldDistance) < maxTime) {
                                    //new 2 hops
                                    hopMap.put(distancesFrom.firstKey(),
                                            new Hop(oldHop.getFrom(), tryWP));

                                    hopMap.put(Matrix.DistanceBetweenMap(tryWP, oldHop.getTo(), matrix),
                                            new Hop(tryWP, oldHop.getTo()));

                                    totalDistance += newDistance;
                                    totalDistance -= oldDistance;
                                    //totalDistance -= 2000;

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
                    result.getItineraries().get(car).add(itinerary);
                    newStart.put(car,itinerary.getWayPointList().get(itinerary.getWayPointList().size()-2));
                    // start new route from last relevant point

                    result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());

                    System.out.println("Iteration " + itCount + " completed in " + (System.currentTimeMillis() - startTime));
                }
                    else
                    {
                        if(wayPoints.size()==1)
                        {
                        WayPoint lastWP = wayPoints.get(0);
                        System.out.println("Only 1 wp left, " + lastWP.getDescription() + ", no route for it");
                        wayPoints.remove(lastWP);}
                        else
                        {
                           System.out.println("skipping...");
                        }
                    }
            }


            }

        // now complete result
        result.setItineraryQty(itCount);
        result.setCars(cars);

        return result;
    }
}
