package GarbageQuest;

import GarbageQuest.entity.*;
import GarbageQuest.mosData.GarbageSiteMD;
import GarbageQuest.mosData.XLSPaidParkings;
import GarbageQuest.service.*;
import GarbageQuest.supplimentary.DistType;
import GarbageQuest.supplimentary.MockTimeSlots;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graphhopper.util.StopWatch;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.round;


public class MatrixLoaderXLS {
    public static void main(String[] args) throws IOException {

        // ----- CONTROLS (set before use) -----------
        String XLSInputFile = "C:\\Users\\User\\Documents\\GD\\park_all.xls";
        boolean filterByRegion = true; // true for Region, false for AO
        String filter = "муниципальный округ Кунцево"; // "Северо-Восточный административный округ", ...

        //OSM data
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";

        String jsonOutputFile = "C:\\Users\\User\\Documents\\GD\\kuntsevo-park2.json";

        boolean runAlgo = true; // execute itinerary routing algo at this stage?
        int avgSpeed = 10; // Average video complex car speed in m/s, 10 is roughly 30 km/h
        int maxTime = 900; // max time for circulation
        int trim = 10000; // correction value for special cases, don't set low (<10000) unless smth wrong
        // ----- CONTROLS END ---------



        // ----- IMPORT FROM XLS & PARSE ------
        StopWatch sw = new StopWatch().start();
        List<XLSPaidParkings> input = XLSImport.loadXLSPaidParkings(XLSInputFile);
        System.out.println("XLS loaded in: " + sw.stop().getSeconds() + " s");

        List<WayPoint> wayPointList = new ArrayList<>();
        for (XLSPaidParkings pp : input)
            if ((pp.getDistrict().equals(filter) && filterByRegion)
                    || (pp.getAdmArea().equals(filter) && !filterByRegion))
        {
            WayPoint wp = new WayPoint();
            wp.setIndex(pp.getGlobal_id());
            wp.setDescription(pp.getAddress());

            String[] digits = pp.getCoordinates().split("[^[0-9.]]+"); // magic regex, see test

            wp.setLat(Double.parseDouble(digits[2]));
            wp.setLon(Double.parseDouble(digits[1]));

            wp.setType(WayPointType.Paid_Parking);
            wayPointList.add(wp);
            }

        System.out.println("\nLoaded " + wayPointList.size() + " sites for " + filter);

        // ---- ADD BASE ----

        wayPointList.add(new WayPoint(0, 55.73235400160589, 37.54848003387452, "BASE",  // example ))
                LocalTime.parse("06:00"), LocalTime.parse("08:00"), Duration.ofMinutes(1),
                WayPointType.Base, 1));


        // ------ NOW FILL THE MATRIX -----

        sw = new StopWatch().start();
        Map<WayPoint,MatrixLineMap> matrix = Matrix.FillGHMulti4Map(
                wayPointList, osmFile, dir, true, true);
        System.out.println("\nMatrix calculated in: " + sw.stop().getSeconds() + " s\n");

        // ----- AND SAVE THE MATRIX IN JSON -----
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try (FileWriter writer = new FileWriter(jsonOutputFile)) {
            for (WayPoint wp : wayPointList)
            {
                MatrixStorageLine msl = new MatrixStorageLine();
                msl.setWayPoint(wp);

                List<Double> dd = new ArrayList<>();
                for(WayPoint wwp : wayPointList)
                {
                    dd.add(matrix.get(wp).getDistances().get(wwp));
                }

                msl.setDistances(dd);

                writer.write(objectMapper.writeValueAsString(msl));
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        System.out.println("Saved as: " + jsonOutputFile);


        // ----- EXECUTE ITINERARY ALGORITHM IF DESIRED -----
        if(runAlgo)
        {
            long startTime = System.currentTimeMillis();
            Result rr = AlgCircularMatrixMapV01.Calculate(wayPointList, matrix, avgSpeed, maxTime,trim);
            long elapsedTime = System.currentTimeMillis() - startTime;

            System.out.println("\n\nTotal distance: " + round(rr.getDistanceTotal()) / 1000 + " km");
            System.out.println("Cars assigned: " + rr.getItineraryQty());
            System.out.println("Calculated in: " + elapsedTime + " ms");
        }
    }
}
