package GarbageQuest;

import GarbageQuest.entity.*;
import GarbageQuest.service.AlgGreedyMatrixMapV01;
import GarbageQuest.service.AlgGreedyMatrixV01;
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

public class ItineraryCalculation {
    public static void main(String[] args) {

        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\Users\\User\\Documents\\GD\\kuntsevo-m.json"; // should exist!

        // random timeslot generation params
        boolean newRandomTime = false; // do we need timeslots recalculation
        int timeStartMin = 6; // opening time, hours in 24h
        int timeStartMax = 7; // non-inclusive
        DistType timeDist = DistType.Descend; // Statistic distribution, Equal, Gaussian, asc&desc Expo
        int intervalMin = 8; // availability from open to close, hours
        int intervalMax = 9; // non-inclusive
        DistType intervalDist = DistType.Ascend;

        int avgSpeed = 10; // Average garbage car speed in m/s, 10 is roughly 30 km/h
        int capacity = 25; // garbage car capacity in abstract units

        //result display options
        boolean fullItinerary = false; // all with waypoints
        boolean shortItinerary = true; // all as summary

        String urlOutputFile = "C:\\Users\\User\\Documents\\GD\\zao002.txt";
        // ----- CONTROLS END ---------


        // ------- RESTORE MATRIX FROM JSON FILE ---------

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());


        List<WayPoint> wayPointList = new ArrayList<>();
        List<MatrixLine> matrix = new ArrayList<>();
        List<MatrixStorageLine> inMatrix = new ArrayList<>();


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

        for (MatrixStorageLine msl : inMatrix) {
            MatrixLine ml = new MatrixLine();
            ml.setWayPoint(msl.getWayPoint());
            List<MatrixElement> distances = new ArrayList<>();
            for (int i = 0; i < msl.getDistances().size(); i++) {
                distances.add(new MatrixElement(wayPointList.get(i), msl.getDistances().get(i)));
            }
            ml.setDistances(distances);
            matrix.add(ml);
        }
        System.out.println("\nRestored matrix from: " +
                jsonInputFile +
                " " +
                wayPointList.size() +
                " " +
                matrix.size());


        // ------- APPLY NEW RANDOM TIMESLOTS IF DESIRED --------
        if(newRandomTime)
        MockTimeSlots.fill(
                wayPointList,
                timeStartMin,
                timeStartMax,
                timeDist,
                intervalMin,
                intervalMax,
                intervalDist
        );

        // ------- NOW RUN ALGO -------

        double startTime = System.currentTimeMillis();
        Result rr = AlgGreedyMatrixV01.Calculate(wayPointList, matrix, avgSpeed, capacity);
        double elapsedTime = System.currentTimeMillis() - startTime;

        // ------- DISPLAY IN DESIRED FORMAT ------
        if(fullItinerary)
        for(Itinerary ii : rr.getItineraries())
        {
            System.out.println("\n\n"+ii.getCar().getDescription());
            System.out.println(ii.getTimeStart()+" - " + ii.getTimeEnd());
            System.out.println(ii.getDistance());
            for(WayPoint ww : ii.getWayPointList())
            {
                System.out.print(ww.getDescription()+" -> ");
            }
        }

        if(shortItinerary)
        for(Itinerary ii : rr.getItineraries())
        {
            System.out.println("\n"+ii.getCar().getDescription());
            System.out.println("No. of waypoints: " + ii.getWayPointList().size());

            System.out.println("Route distance: " + round(ii.getDistance())/1000);
            System.out.println("Avg hop: " + round(ii.getDistance()/(ii.getWayPointList().size())));
        }

        System.out.println("\n\nTotal distance: " + round(rr.getDistanceTotal()) / 1000 + " km");
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
