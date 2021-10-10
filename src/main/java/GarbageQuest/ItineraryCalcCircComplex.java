package GarbageQuest;

import GarbageQuest.entity.*;
import GarbageQuest.service.*;
import GarbageQuest.supplimentary.DistType;
import GarbageQuest.supplimentary.MockTimeSlots;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.gpx.GpxConversions;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;

public class ItineraryCalcCircComplex {
    public static void main(String[] args) {

        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\Users\\User\\Documents\\GD\\cheremushki-park-good.json"; // should exist!

        //OSM data
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";

        int maxTime = 600000; // max circulation time in MILLIseconds
        int noOfCars = 2;

        String urlOutputFile = "C:\\Users\\User\\Documents\\GD\\cheremushki-park-good001.txt";
        String outDir ="C:\\Users\\User\\Documents\\GD\\tracks\\cheremushki";
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


        // ------- NOW RUN ALGO -------

        double startTime = System.currentTimeMillis();
        ResultComplex rr = AlgCircularMatrixMapCompl.Calculate(wayPointList, matrix, maxTime, noOfCars);
        double elapsedTime = System.currentTimeMillis() - startTime;


        System.out.println("\n\nTotal time: " + round(rr.getDistanceTotal()) / 1000 + " sec");
        System.out.println("Routes: " + rr.getItineraryQty());
        System.out.println("Calculated in: " + elapsedTime + " ms");




        // ----- SAVE RESULTS FOR VISUALISATION IN GH ------
        //https://graphhopper.com/maps/?
        // point=55.726627%2C37.529984&
        // point=55.723702%2C37.525392&
        // point=55.720101%2C37.515907&
        // locale=ru-RU&elevation=true&profile=car&use_miles=false&selected_detail=Elevation&layer=Omniscale

        try (FileWriter writer = new FileWriter(urlOutputFile))
        {
            for(Car car: rr.getCars())
            {
                writer.write("\n\nCar: " + car + "\n\n");
                for (ItineraryCirc ii : rr.getItineraries().get(car))
                {
                    String url = "https://graphhopper.com/maps/?";
                    for (WayPoint wp : ii.getWayPointList()) {
                        url += "point=";
                        url += wp.getLat();
                        url += "%2C";
                        url += wp.getLon();
                        url += "&";
                    }
                    url += "locale=ru-RU&profile=car&use_miles=false";

                    writer.write(url);
                    writer.write("\n\n");
                }
            }
            writer.flush();
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage());
        }
        System.out.println("\nSaved as: " + urlOutputFile);

        // SAVE RESULTS AS GPX TRACKS

        //GraphHopper hopper = new GraphHopperOSM().forServer();
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);
        //hopper.setEncodingManager(EncodingManager.create("car|turn_costs=true"));

        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("shortest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"), new CHProfile("car2"));
        hopper.importOrLoad();

        for(Car car: rr.getCars())
        {
            for (ItineraryCirc ii : rr.getItineraries().get(car))
            {
                String GPXFileName = outDir + "\\car" + (rr.getCars().indexOf(car)+1) + "-" +
                        (rr.getItineraries().get(car).indexOf(ii)+1) + ".gpx";

                GHRequest req = new GHRequest()
                        .setProfile("car2")
                        .setAlgorithm(Parameters.Algorithms.ASTAR_BI);

                List<String> curbSides = new ArrayList<>();
                for (WayPoint wp : ii.getWayPointList())
                {
                    req.addPoint(new GHPoint(wp.getLat(), wp.getLon()));
                    curbSides.add("right");
                }

                req.setCurbsides(curbSides);

                //req.putHint("instructions", false);
                //req.putHint("calc_points", false);
                //req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

                GHResponse res = hopper.route(req);

                if (res.hasErrors()) {
                    throw new RuntimeException(res.getErrors().toString());
                }

                String trackName = "Car # " + (rr.getCars().indexOf(car)+1) + " route # " +
                        (rr.getItineraries().get(car).indexOf(ii)+1);

                String gpx = GpxConversions.createGPX(
                        res.getBest().getInstructions(),
                        trackName,
                        0,
                        false,
                        false,
                        true,
                        true,
                        "version 1.0",
                        res.getBest().getInstructions().getTr());

                //System.out.println(gpx);

                try (FileWriter writer = new FileWriter(GPXFileName)) {

                    writer.write(gpx);
                    writer.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
