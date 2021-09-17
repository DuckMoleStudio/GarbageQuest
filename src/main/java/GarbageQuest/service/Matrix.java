package GarbageQuest.service;

import GarbageQuest.entity.MatrixElement;
import GarbageQuest.entity.MatrixLine;
import GarbageQuest.entity.WayPoint;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Parameters;

import java.util.ArrayList;
import java.util.List;

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
        int size = wayPoints.size();
        List<MatrixLine> matrix = new ArrayList<>();

        GraphHopper hopper = new GraphHopperOSM().forServer();

        hopper.setEncodingManager(EncodingManager.create("car"));
        hopper.setGraphHopperLocation(dir);
        hopper.setDataReaderFile(osmFile);

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
                    synchronized (matrix) {matrix.add(line);};
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

    public static List<MatrixLine> FillGHSingle(List<WayPoint> wayPoints)
    // using Graph Hopper on real map, single thread
    {
        List<MatrixLine> matrix = new ArrayList<>();

        GraphHopper hopper = new GraphHopperOSM().forServer();

        hopper.setEncodingManager(EncodingManager.create("car"));
        hopper.setGraphHopperLocation("local/graphhopper");
        hopper.setDataReaderFile("C:/Users/User/Downloads/RU-MOW.osm.pbf");

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
            if(ml.getWayPoint().equals(start))
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
            if(ml.getWayPoint().equals(start))
            {
                for(MatrixElement me: ml.getDistances())
                {
                    if(me.getWayPoint().equals(end))
                    {
                        return me.getDistance();
                    }
                }
            }
        }
        return Double.POSITIVE_INFINITY;
    }
}
