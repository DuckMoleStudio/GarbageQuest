package GarbageQuest;

import GarbageQuest.entity.*;
import GarbageQuest.service.*;
import GarbageQuest.supplimentary.DistType;
import GarbageQuest.supplimentary.MockTimeSlots;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;

public class ItineraryCalcCircle {
    public static void main(String[] args) {

        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\Users\\User\\Documents\\GD\\hamovniki-park-good.json"; // should exist!



        int maxTime = 1200000; // max circulation time in MILLIseconds
        int trim = 10000000; // correction value for special cases, don't set low (<1000000) unless smth wrong

        //result display options
        boolean fullItinerary = false; // all with waypoints
        boolean shortItinerary = true; // all as summary

        String urlOutputFile = "C:\\Users\\User\\Documents\\GD\\hamovniki-park-good001.txt";
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
        Result rr = AlgCircularMatrixMapV02.Calculate(wayPointList, matrix, maxTime);
        double elapsedTime = System.currentTimeMillis() - startTime;

        // ------- DISPLAY IN DESIRED FORMAT ------
        if(fullItinerary)
            for(Itinerary ii : rr.getItineraries())
            {
                System.out.println("\n\n"+ii.getCar().getDescription());

                System.out.println(ii.getDistance()/1000);
                for(WayPoint ww : ii.getWayPointList())
                {
                    System.out.print(ww.getIndex()+" -> ");
                }
            }

        if(shortItinerary)
            for(Itinerary ii : rr.getItineraries())
            {
                System.out.println("\n"+ii.getCar().getDescription());
                System.out.println("No. of waypoints: " + ii.getWayPointList().size());

                //System.out.println("Route distance: " + round(ii.getDistance())/1000);
                System.out.println("Route circulation time: " + ii.getDistance());
                System.out.println("Avg hop: " + round(ii.getDistance()/(ii.getWayPointList().size())));
            }

        System.out.println("\n\nTotal time: " + round(rr.getDistanceTotal()) / 1000 + " sec");
        System.out.println("Cars assigned: " + rr.getItineraryQty());
        System.out.println("Calculated in: " + elapsedTime + " ms");




        // ----- SAVE RESULTS FOR VISUALISATION ------
        //https://graphhopper.com/maps/?
        // point=55.726627%2C37.529984&
        // point=55.723702%2C37.525392&
        // point=55.720101%2C37.515907&
        // locale=ru-RU&elevation=true&profile=car&use_miles=false&selected_detail=Elevation&layer=Omniscale

        try (FileWriter writer = new FileWriter(urlOutputFile))
        {
            for (Itinerary ii : rr.getItineraries())
            {
                String url = "https://graphhopper.com/maps/?";
                for(WayPoint wp: ii.getWayPointList())
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
