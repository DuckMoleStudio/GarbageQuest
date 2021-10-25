package GarbageQuest;

import GarbageQuest.entity.*;
import GarbageQuest.service.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.gpx.GpxConversions;

import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer;
import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer.Label;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;
import com.graphhopper.jsprit.io.problem.VrpXMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;

public class JspritGames{
    public static void main(String[] args) {

        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\Users\\User\\Documents\\GD\\gagarinskii-park-good.json"; // should exist!

        //OSM data
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";

        int maxTime = 600000; // max circulation time in MILLIseconds
        int noOfCars = 2;
        boolean good=true; // good -- with relevant access from right curbside
        int capacity = 30;

        String urlOutputFile = "C:\\Users\\User\\Documents\\GD\\gagarinskii-park-good002.txt";
        String outDir ="C:\\Users\\User\\Documents\\GD\\tracks\\gagarinskii";
        // ----- CONTROLS END ---------


        // ------- RESTORE MATRIX FROM JSON FILE ---------

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());


        List<WayPoint> wayPointList = new ArrayList<>();
        List<MatrixStorageLine> inMatrix = new ArrayList<>();

        Map<WayPoint, MatrixLineMap> matrix = new HashMap<>();


        String inString = null;
        try {
            inString = new String(Files.readAllBytes(Paths.get(jsonInputFile)));
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        List<String> inStrings = Arrays.stream(inString.split("\n")).collect(Collectors.toList());
        for (String ss : inStrings) {
            try {
                inMatrix.add(objectMapper.readValue(ss, MatrixStorageLine.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        for (MatrixStorageLine msl : inMatrix) {
            wayPointList.add(msl.getWayPoint());
        }

        for (MatrixStorageLine msl : inMatrix)
        {
            MatrixLineMap ml = new MatrixLineMap();
            ml.setDistances(new HashMap<>());
            for (int i = 0; i < msl.getDistances().size(); i++)
            {
                ml.getDistances().put(wayPointList.get(i), msl.getDistances().get(i));
            }
            matrix.put(msl.getWayPoint(),ml);

        }
        System.out.println("\nRestored matrix from: " +
                jsonInputFile +
                " with " +
                wayPointList.size() +
                " points" );

        // --- JSPRIT -----

        List<Service> services = new ArrayList<>();
        WayPoint startWP = new WayPoint();
        for(WayPoint c_wp: wayPointList)
        {
            if (c_wp.getType() == WayPointType.Base) {
                startWP = c_wp;
            }
            else {
                services.add(Service.Builder
                        .newInstance(String.valueOf(c_wp.getIndex()))
                        .addSizeDimension(0, 1)
                        .setLocation(Location.newInstance(String.valueOf(c_wp.getIndex())))
                        .build());
            }
        }

        String start = String.valueOf(startWP.getIndex());


        /*
         * some preparation - create output folder
         */
        File dir_j = new File("output");
        // if the directory does not exist, create it
        if (!dir_j.exists()) {
            System.out.println("creating directory ./output");
            boolean result = dir_j.mkdir();
            if (result) System.out.println("./output created");
        }

        /*
         * get a vehicle type-builder and build a type
         */
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleTypeRita")
                .addCapacityDimension(0, capacity);
        VehicleType vehicleTypeRita = vehicleTypeBuilder.build();

        /*
         * get a vehicle-builder and build a vehicle
         */
        Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicleRita");
        vehicleBuilder.setStartLocation(Location.newInstance(start));
        vehicleBuilder.setType(vehicleTypeRita);
        VehicleImpl vehicleRita = vehicleBuilder.build();

        /*
         * build services at the required locations, each with a capacity-demand of 1.
         */



        //define a matrix-builder building a NON-symmetric matrix
        VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix
                .Builder.newInstance(false);
        for(int jj=0;jj<wayPointList.size();jj++)
        for(int kk=0;kk<wayPointList.size();kk++)
        if(jj!=kk)
        {
            costMatrixBuilder.addTransportDistance(
                    String.valueOf(wayPointList.get(jj).getIndex()),
                    String.valueOf(wayPointList.get(kk).getIndex()),
                    Matrix.DistanceBetweenMap(wayPointList.get(jj),wayPointList.get(kk),matrix));
            costMatrixBuilder.addTransportTime(
                    String.valueOf(wayPointList.get(jj).getIndex()),
                    String.valueOf(wayPointList.get(kk).getIndex()),
                    Matrix.DistanceBetweenMap(wayPointList.get(jj),wayPointList.get(kk),matrix));
        }
        VehicleRoutingTransportCosts costMatrix = costMatrixBuilder.build();


        // --- SET UP THE ROUTING PROBLEM ----
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance()
                .setFleetSize(VehicleRoutingProblem.FleetSize.INFINITE)
                .setRoutingCost(costMatrix);

        vrpBuilder.addVehicle(vehicleRita);
        for(Service ss: services)
        {
            vrpBuilder.addJob(ss);
        }

        VehicleRoutingProblem problem = vrpBuilder.build();

        /*
         * get the algorithm out-of-the-box.
         */
        VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(problem);
        algorithm.setMaxIterations(100);

        /*
         * and search a solution
         */
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

        /*
         * get the best
         */
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

        SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);

        // --- PARSE SOLUTION TO OUR RESULT ----
        Result result = new Result();
        result.setMethodUsed("Jsprit Algorithm, simple VRP");
        List<Itinerary> ii = new ArrayList<>();
        result.setItineraries(ii);

        List<VehicleRoute> list = new ArrayList<VehicleRoute>(bestSolution.getRoutes());
        Collections.sort(list , new com.graphhopper.jsprit.core.util.VehicleIndexComparator());

        int routeNu = 1;
        for (VehicleRoute route : list)
        {
            Itinerary curItinerary = new Itinerary();
            curItinerary.setCar(new Car(route.getVehicle().getId() + (routeNu),
                    WayPointType.Paid_Parking, 0));

            WayPoint curWP = startWP;
            List<WayPoint> ll = new ArrayList<>();
            ll.add(curWP);
            curItinerary.setWayPointList(ll);
            double costs = 0;


            TourActivity prevAct = route.getStart();
            for (TourActivity act : route.getActivities())
            {
                String jobId;
                if (act instanceof TourActivity.JobActivity)
                {
                    jobId = ((TourActivity.JobActivity) act).getJob().getId();

                    for(WayPoint wwp: wayPointList)
                    if(String.valueOf(wwp.getIndex()).equals(jobId))
                        {
                            curItinerary.getWayPointList().add(wwp);
                        }

                }
                else
                {
                    jobId = "-";
                }
                double c = problem.getTransportCosts().getTransportCost(
                        prevAct.getLocation(),
                        act.getLocation(),
                        prevAct.getEndTime(),
                        route.getDriver(),
                        route.getVehicle());
                c += problem.getActivityCosts().getActivityCost(
                        act,
                        act.getArrTime(),
                        route.getDriver(),
                        route.getVehicle());
                costs += c;

                prevAct = act;
            }



            double c = problem.getTransportCosts().getTransportCost(
                    prevAct.getLocation(),
                    route.getEnd().getLocation(),
                    prevAct.getEndTime(),
                    route.getDriver(),
                    route.getVehicle());
            c += problem.getActivityCosts().getActivityCost(
                    route.getEnd(),
                    route.getEnd().getArrTime(),
                    route.getDriver(),
                    route.getVehicle());
            costs += c;


            routeNu++;

            // ---- complete itinerary ----
            curItinerary.getWayPointList().add(startWP);
            curItinerary.setDistance(costs);
            result.getItineraries().add(curItinerary);
            result.setDistanceTotal(result.getDistanceTotal() + curItinerary.getDistance());

        }
        // ---- complete result -----
        result.setItineraryQty(routeNu-1);

        System.out.println("\n\nTotal time: " + round(result.getDistanceTotal()) / 1000 + " sec");
        System.out.println("Routes: " + result.getItineraryQty());

        // ----- SAVE RESULTS FOR VISUALISATION ------
        //https://graphhopper.com/maps/?
        // point=55.726627%2C37.529984&
        // point=55.723702%2C37.525392&
        // point=55.720101%2C37.515907&
        // locale=ru-RU&elevation=true&profile=car&use_miles=false&selected_detail=Elevation&layer=Omniscale

        try (FileWriter writer = new FileWriter(urlOutputFile))
        {
            for (Itinerary iii : result.getItineraries())
            {
                String url = "https://graphhopper.com/maps/?";
                for(WayPoint wp: iii.getWayPointList())
                {
                    url+="point=";
                    url+=wp.getLat();
                    url+="%2C";
                    url+=wp.getLon();
                    url+="&";
                }
                url+="locale=ru-RU&profile=car&use_miles=false";

                writer.write(url);
                writer.write("\n\n");
            }
            writer.flush();
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage());
        }
        System.out.println("\nSaved as: " + urlOutputFile);





    }
    }
