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

import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;

public class ItineraryCalcCamera1 {
    public static void main(String[] args) {

        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\Users\\User\\Documents\\GD\\ah-park-good.json"; // should exist!

        //OSM data
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";


        int noOfCars = 2;
        boolean good=true; // good -- with relevant access from right curbside
        int capacity = 840;
        int iterations = 50; // these 2 for jsprit algo

        String urlOutputFile = "C:\\Users\\User\\Documents\\GD\\ah-park-good002j.txt";
        String outDir ="C:\\Users\\User\\Documents\\GD\\tracks\\ah";
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
        Result rr = AlgJspritGeneral.Calculate(wayPointList, matrix, capacity, iterations);
        //Result rr = AlgGreedySimpleV01.Calculate(wayPointList, matrix, noOfCars);
        double elapsedTime = System.currentTimeMillis() - startTime;


        System.out.println("\n\nTotal time: " + round(rr.getDistanceTotal()) / 1000 + " sec");
        System.out.println("Routes: " + rr.getItineraryQty());
        System.out.println("Calculated in: " + elapsedTime + " ms");




        // ----- SAVE RESULTS FOR VISUALISATION ------
        //https://graphhopper.com/maps/?
        // point=55.726627%2C37.529984&
        // point=55.723702%2C37.525392&
        // point=55.720101%2C37.515907&
        // locale=ru-RU&elevation=true&profile=car&use_miles=false&selected_detail=Elevation&layer=Omniscale

        try (FileWriter writer = new FileWriter(urlOutputFile))
        {
            for (Itinerary iii : rr.getItineraries())
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

        // SAVE RESULTS AS GPX TRACKS

        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);


        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("shortest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"), new CHProfile("car2"));
        hopper.importOrLoad();


        int j=0;
            for (Itinerary ii : rr.getItineraries())
            {
                String GPXFileName = outDir + "\\car0" + (j++) + ".gpx";

                GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);

                if (good) {
                    req.setProfile("car2");
                } else {
                    req.setProfile("car1");
                }

                List<String> curbSides = new ArrayList<>();
                for (WayPoint wp : ii.getWayPointList())
                {
                    req.addPoint(new GHPoint(wp.getLat(), wp.getLon()));
                    curbSides.add("right");
                }

                if(good) req.setCurbsides(curbSides);

                //req.putHint("instructions", false);
                //req.putHint("calc_points", false);
                //req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

                GHResponse res = hopper.route(req);

                if (res.hasErrors()) {
                    throw new RuntimeException(res.getErrors().toString());
                }

                String trackName = "Car # " + (j);

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

                System.out.println("Saving " + GPXFileName + "...");

                try (FileWriter writer = new FileWriter(GPXFileName)) {

                    writer.write(gpx);
                    writer.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    }
}
