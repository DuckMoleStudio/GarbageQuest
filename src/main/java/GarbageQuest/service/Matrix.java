package GarbageQuest.service;

import GarbageQuest.entity.*;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;

import com.graphhopper.util.Parameters;

import java.util.*;

public class Matrix {

    public static List<MatrixLine> FillSimple(List<WayPoint> wayPoints) // on chequered simplified map
    {
        List<MatrixLine> matrix = new ArrayList<>();

        for(int i=0; i < wayPoints.size(); i++)
        {
            MatrixLine line = new MatrixLine();
            line.setWayPoint(wayPoints.get(i));
            List<MatrixElement> ll = new ArrayList<>();

            for(int j=0; j < wayPoints.size(); j++)
            {
                if(i==j)
                {
                    ll.add(new MatrixElement(wayPoints.get(j),Double.POSITIVE_INFINITY));
                }
                else
                {
                    ll.add(new MatrixElement(wayPoints.get(j),CalcDistanceSimple.Calc(
                            wayPoints.get(i).getLat(),
                            wayPoints.get(i).getLon(),
                            wayPoints.get(j).getLat(),
                            wayPoints.get(j).getLon()
                    )));
                }
            }
            line.setDistances(ll);
            matrix.add(line);
        }
        return matrix;
    }



    public static List<MatrixLine> FillGHMulti4(List<WayPoint> wayPoints, String osmFile, String dir)
    // using Graph Hopper on real map, with 4 threads
    {
        List<MatrixLine> matrix = new ArrayList<>();

        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(
                new Profile("car").setVehicle("car").setWeighting("shortest")
        );
        // this enables speed mode for the profile we call "car" here
        hopper.getCHPreparationHandler().setCHProfiles(
                new CHProfile("car")
        );
        hopper.importOrLoad();

        int section = (int)Math.ceil(wayPoints.size() / 4);

        class sub {
            void fill (int from, int to){
                for(int i=from; i < to; i++)
                {
                    MatrixLine line = new MatrixLine();
                    line.setWayPoint(wayPoints.get(i));
                    List<MatrixElement> ll = new ArrayList<>();

                    for(int j=0; j < wayPoints.size(); j++)
                    {
                        if(i==j)
                        {
                            ll.add(new MatrixElement(wayPoints.get(j),Double.POSITIVE_INFINITY));
                        }
                        else
                        {
                            GHRequest req = new GHRequest(
                                    wayPoints.get(i).getLat()
                                    , wayPoints.get(i).getLon()
                                    , wayPoints.get(j).getLat()
                                    , wayPoints.get(j).getLon())
                                    .setProfile("car")
                                    .setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                            req.putHint("instructions", false);
                            req.putHint("calc_points", false);
                            GHResponse res = hopper.route(req);
                            double distance = res.getBest().getDistance();

                            ll.add(new MatrixElement(wayPoints.get(j),distance));
                        }
                    }
                    if(i%(wayPoints.size()/50) == 0)
                    System.out.print("."); // progress indicator, optimized for 50 dots

                    line.setDistances(ll);
                    synchronized (matrix) {matrix.add(line);}
                }
            }
        }

        Thread one = new Thread(new Runnable() {
            @Override
            public void run() {
               sub sub1 = new sub();
               sub1.fill(0,section);
            }
        });

        Thread two = new Thread(new Runnable() {
            @Override
            public void run() {
                sub sub2 = new sub();
                sub2.fill(section,section*2);
            }
        });

        Thread three = new Thread(new Runnable() {
            @Override
            public void run() {
                sub sub3 = new sub();
                sub3.fill(section*2,section*3);
            }
        });

        Thread four = new Thread(new Runnable() {
            @Override
            public void run() {
                sub sub4 = new sub();
                sub4.fill(section*3,wayPoints.size());
            }
        });

// start threads
        one.start();
        two.start();
        three.start();
        four.start();

// Wait for threads above to finish
        try{
            one.join();
            two.join();
            three.join();
            four.join();
        }
        catch (InterruptedException e)
        {
            System.out.println("Interrupt Occurred");
            e.printStackTrace();
        }

        return matrix;
    }

    public static Map<WayPoint, MatrixLineMap> FillGHMulti4Map(
            List<WayPoint> wayPoints, String osmFile, String dir, boolean turns, boolean curbs, boolean time)
    // using Graph Hopper on real map, with 4 threads, store in HashMap
    {

        Map<WayPoint, MatrixLineMap> matrix = new HashMap<>();

        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("shortest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );

        // this enables speed mode for the profile we call "car" here
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"),new CHProfile("car2"));

        hopper.importOrLoad();

        String profile = "car1";
        if(turns) profile = "car2";

        int section = (int)Math.ceil(wayPoints.size() / 4);

        String finalProfile = profile;
        class sub {
            void fill (int from, int to){
                for(int i=from; i < to; i++)
                {
                    MatrixLineMap line = new MatrixLineMap();
                    line.setDistances(new HashMap<>());

                    for(int j=0; j < wayPoints.size(); j++)
                    {
                        if(i==j)
                        {
                            line.getDistances().put(wayPoints.get(j),Double.POSITIVE_INFINITY);
                        }
                        else
                        {
                            GHRequest req = new GHRequest(
                                    wayPoints.get(i).getLat()
                                    , wayPoints.get(i).getLon()
                                    , wayPoints.get(j).getLat()
                                    , wayPoints.get(j).getLon())
                                    .setProfile(finalProfile)
                                    .setAlgorithm(Parameters.Algorithms.ASTAR_BI);

                            if(curbs)
                            {
                                req.setCurbsides(Arrays.asList("right", "right"));
                                //req.putHint("u_turn_costs", 6000);
                            }

                            req.putHint("instructions", false);
                            req.putHint("calc_points", false);
                            req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

                            GHResponse res = hopper.route(req);

                            double distance = Double.POSITIVE_INFINITY;

                            if (res.hasErrors()) {
                               // throw new RuntimeException(res.getErrors().toString());

                            }
                            else {

                                if (time) {distance = res.getBest().getTime();}
                                else distance = res.getBest().getDistance();
                            }


                            line.getDistances().put(wayPoints.get(j),distance);
                        }
                    }

                    int div = 50;
                    if(wayPoints.size() <= 50) div = wayPoints.size()-1;
                    if(i%(wayPoints.size()/div) == 0)
                        System.out.print("."); // progress indicator, optimized for 50 dots or fewer

                    synchronized (matrix) {matrix.put(wayPoints.get(i),line);}
                }
            }
        }

        Thread one = new Thread(new Runnable() {
            @Override
            public void run() {
                sub sub1 = new sub();
                sub1.fill(0,section);
            }
        });

        Thread two = new Thread(new Runnable() {
            @Override
            public void run() {
                sub sub2 = new sub();
                sub2.fill(section,section*2);
            }
        });

        Thread three = new Thread(new Runnable() {
            @Override
            public void run() {
                sub sub3 = new sub();
                sub3.fill(section*2,section*3);
            }
        });

        Thread four = new Thread(new Runnable() {
            @Override
            public void run() {
                sub sub4 = new sub();
                sub4.fill(section*3,wayPoints.size());
            }
        });

// start threads
        one.start();
        two.start();
        three.start();
        four.start();

// Wait for threads above to finish
        try{
            one.join();
            two.join();
            three.join();
            four.join();
        }
        catch (InterruptedException e)
        {
            System.out.println("Interrupt Occurred");
            e.printStackTrace();
        }

        return matrix;
    }

    public static DoubleMatrix FillGHDoubleTime(
            List<WayPoint> wayPoints, String osmFile, String dir, WayPoint base)
    // using Graph Hopper on real map, store in two (maybe) HashMaps, time-based
    {

        Map<WayPoint, MatrixLineMap> matrixGood = new HashMap<>();
        Map<WayPoint, MatrixLineMap> matrixBad = new HashMap<>();
        List<WayPoint> wayPointsGood = new ArrayList<>();
        List<WayPoint> wayPointsBad = new ArrayList<>();

        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("shortest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );

        // this enables speed mode for the profile we call "car" here
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"),new CHProfile("car2"));

        hopper.importOrLoad();

        for(WayPoint wp : wayPoints)
        {
            GHRequest req = new GHRequest(
                    base.getLat()
                    , base.getLon()
                    , wp.getLat()
                    , wp.getLon())
                    .setProfile("car2")
                    .setCurbsides(Arrays.asList("any", "right"))
                    .setAlgorithm(Parameters.Algorithms.ASTAR_BI);

            req.putHint("instructions", false);
            req.putHint("calc_points", false);
            //req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

            GHResponse res = hopper.route(req);

            if (res.hasErrors())  //throw new RuntimeException(res.getErrors().toString());
            {wayPointsBad.add(wp);}
            else wayPointsGood.add(wp);

        }
        if(wayPointsGood.size()>0)
        {
            wayPointsGood.add(base);
            System.out.println("\n\nCalculating matrix for " + wayPointsGood.size() + " relevant points\n");
            matrixGood = Matrix.FillGHMulti4Map(wayPointsGood,osmFile,dir,true,true,true);
        }

        if(wayPointsBad.size()>0)
        {
            wayPointsBad.add(base);
            System.out.println("\n\nCalculating matrix for " + wayPointsBad.size() + " irrelevant points\n");
            matrixBad = Matrix.FillGHMulti4Map(wayPointsBad,osmFile,dir,false,false,true);
        }


        return new DoubleMatrix(matrixGood,matrixBad,wayPointsGood,wayPointsBad);
    }

    public static List<MatrixLine> FillGHSingle(List<WayPoint> wayPoints)
    // using Graph Hopper on real map, single thread
    {
        List<MatrixLine> matrix = new ArrayList<>();

        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile("C:/Users/User/Downloads/RU-MOW.osm.pbf");
        hopper.setGraphHopperLocation("local/graphhopper");

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(
                new Profile("car").setVehicle("car").setWeighting("shortest")
        );
        // this enables speed mode for the profile we call "car" here
        hopper.getCHPreparationHandler().setCHProfiles(
                new CHProfile("car")
        );
        hopper.importOrLoad();


        class sub {
            void fill (int from, int to){
                for(int i=from; i < to; i++)
                {
                    MatrixLine line = new MatrixLine();
                    line.setWayPoint(wayPoints.get(i));
                    List<MatrixElement> ll = new ArrayList<>();

                    for(int j=0; j < wayPoints.size(); j++)
                    {
                        if(i==j)
                        {
                            ll.add(new MatrixElement(wayPoints.get(j),Double.POSITIVE_INFINITY));
                        }
                        else
                        {
                            GHRequest req = new GHRequest(
                                    wayPoints.get(i).getLat()
                                    , wayPoints.get(i).getLon()
                                    , wayPoints.get(j).getLat()
                                    , wayPoints.get(j).getLon())
                                    .setProfile("car")
                                    .setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                            req.putHint("instructions", false);
                            req.putHint("calc_points", false);
                            GHResponse res = hopper.route(req);
                            double distance = res.getBest().getDistance();
                            System.out.println("Distance = " + distance);


                            ll.add(new MatrixElement(wayPoints.get(j),distance));
                        }
                    }
                    line.setDistances(ll);
                    matrix.add(line);
                }
            }
        }

                sub sub1 = new sub();
                sub1.fill(0,wayPoints.size());

        return matrix;
    }



    public static MatrixElement Nearest(WayPoint start, List<MatrixLine> matrix, List<WayPoint> existing)
    {
        MatrixElement result = new MatrixElement();
        for(MatrixLine ml: matrix)
        {
            if(ml.getWayPoint()==(start)) // not equals for performance reason
            {
                double minDistance = Double.POSITIVE_INFINITY;
                for(MatrixElement me: ml.getDistances())
                {
                    if(me.getDistance()<minDistance && existing.contains(me.getWayPoint()))
                    {
                        minDistance = me.getDistance();
                        result = me;
                    }
                }
            }
        }
        return result;
    }



    public static double DistanceBetween(WayPoint start, WayPoint end, List<MatrixLine> matrix)
    {
        for(MatrixLine ml: matrix)
        {
            if(ml.getWayPoint()==(start)) // not equals for performance reason
            {
                for(MatrixElement me: ml.getDistances())
                {
                    if(me.getWayPoint()==(end)) // not equals for performance reason
                    {
                        return me.getDistance();
                    }
                }
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    public static double DistanceBetweenMap(WayPoint start, WayPoint end, Map<WayPoint,MatrixLineMap> matrix)
    {
        return matrix.get(start).getDistances().get(end);
    }

    public static MatrixElement Nearest2PairMap(
            WayPoint start,
            WayPoint end,
            Map<WayPoint,MatrixLineMap> matrix,
            List<WayPoint> existing,
            int trim)
    {
        MatrixLineMap mlFrom = matrix.get(start);

        Set<Map.Entry<WayPoint,Double>> distancesFrom = mlFrom.getDistances().entrySet();

        //MatrixElement result = new MatrixElement();
        //double minDistance = Double.POSITIVE_INFINITY;
        MatrixElement result = Matrix.NearestMap(start,matrix,existing);
        double minDistance = result.getDistance() +
                Matrix.DistanceBetweenMap(result.getWayPoint(),end,matrix);
        result.setDistance(minDistance);
        //System.out.println("Nearest dist " + minDistance);

        for (Map.Entry<WayPoint,Double> newME: distancesFrom)
        {
            if(existing.contains(newME.getKey()))
            {
                if(((newME.getValue() + Matrix.DistanceBetweenMap(newME.getKey(),end,matrix)) + trim)
                        < minDistance) // trim added!!
                {
                    minDistance = newME.getValue() + Matrix.DistanceBetweenMap(newME.getKey(),end,matrix);
                    result.setDistance(minDistance); // combined distance here, pay attention
                    result.setWayPoint(newME.getKey());
                }
            }
        }
        //System.out.println("Resulting dist " + minDistance + "\n");
        return result;
    }

    public static MatrixElement NearestMap(
            WayPoint start,
            Map<WayPoint,MatrixLineMap> matrix,
            List<WayPoint> existing)
    {
        MatrixLineMap ml = matrix.get(start);
        Set<Map.Entry<WayPoint,Double>> distances = ml.getDistances().entrySet();
        MatrixElement result = new MatrixElement();
        double minDistance = Double.POSITIVE_INFINITY;

        for (Map.Entry<WayPoint,Double> me: distances)
        {
            if(me.getValue() < minDistance && existing.contains(me.getKey()))
            {
                minDistance = me.getValue();
                result.setDistance(me.getValue());
                result.setWayPoint(me.getKey());
            }
        }


        return result;
    }


}
