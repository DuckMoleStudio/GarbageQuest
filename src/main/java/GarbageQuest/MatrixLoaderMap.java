package GarbageQuest;

import GarbageQuest.entity.*;
import GarbageQuest.mosData.GarbageSiteMD;
import GarbageQuest.service.AlgGreedyMatrixMapV01;
import GarbageQuest.service.AlgGreedyMatrixV01;
import GarbageQuest.service.Matrix;
import GarbageQuest.service.MosDataImport;
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


public class MatrixLoaderMap {
    public static void main(String[] args) {
        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\Users\\User\\Documents\\GD\\MDjson-ALL.json";
        boolean filterByRegion = true; // true for Region, false for AO
        String filter = "район Кунцево"; // "Северо-Восточный административный округ", ...

        // random timeslot generation params
        int timeStartMin = 6; // opening time, hours in 24h
        int timeStartMax = 12; // non-inclusive
        DistType timeDist = DistType.Descend; // Statistic distribution, Equal, Gaussian, asc&desc Expo
        int intervalMin = 1; // availability from open to close, hours
        int intervalMax = 7; // non-inclusive
        DistType intervalDist = DistType.Ascend;

        //OSM data
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";

        String jsonOutputFile = "C:\\Users\\User\\Documents\\GD\\kuntsevo-m.json";

        boolean runAlgo = true; // execute itinerary routing algo at this stage?
        int avgSpeed = 10; // Average garbage car speed in m/s, 10 is roughly 30 km/h
        int capacity = 25; // garbage car capacity in abstract units
        // ----- CONTROLS END ---------


        System.out.println("Criteria : " + filter);
        // ----- IMPORT FROM MOSDATA JSON & FILTER ------
        List<GarbageSiteMD> input = MosDataImport.Load(jsonInputFile);
        List<WayPoint> wayPointList = new ArrayList<>();
        for (GarbageSiteMD gs : input) {
            if ((gs.getYardLocation()[0].getDistrict().equals(filter) && filterByRegion)
                    || (gs.getYardLocation()[0].getAdmArea().equals(filter) && !filterByRegion)) {
                WayPoint wp = new WayPoint();
                wp.setIndex(gs.getGlobal_id());
                wp.setLat(gs.getGeoData().getCoordinates()[1]);
                wp.setLon(gs.getGeoData().getCoordinates()[0]);
                wp.setDescription(gs.getYardName());
                wp.setDuration(Duration.ofMinutes(10));
                wp.setType(WayPointType.Garbage_Site);
                wp.setCapacity(1);

                wayPointList.add(wp);
            }
        }
        System.out.println("\nLoaded " + wayPointList.size() + " sites\n");

        // ---- ADD BASE & DUMP ----
        wayPointList.add(new WayPoint(0, 55.766, 37.532, "BASE",  //MKM-logistika
                LocalTime.parse("06:00"), LocalTime.parse("08:00"), Duration.ofMinutes(1),
                WayPointType.Base, 1));
        wayPointList.add(new WayPoint(1, 55.769, 37.5, "DUMP SITE", // 55.769 37.5 Силикатный  55.376 39 Egoryevsk
                LocalTime.of(timeStartMin, 0), LocalTime.of(timeStartMax + intervalMax, 0),
                Duration.ofMinutes(30),
                WayPointType.Garbage_Dump, 1));

        // ---- NOW INIT RANDOM TIMESLOTS -----
        MockTimeSlots.fill(
                wayPointList,
                timeStartMin,
                timeStartMax,
                timeDist,
                intervalMin,
                intervalMax,
                intervalDist
        );

        // ------ NOW FILL THE MATRIX -----


        StopWatch sw = new StopWatch().start();
        Map<WayPoint,MatrixLineMap> matrix = Matrix.FillGHMulti4Map(
                wayPointList, osmFile, dir, false, false);
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
            Result rr = AlgGreedyMatrixMapV01.Calculate(wayPointList, matrix, avgSpeed, capacity);
            long elapsedTime = System.currentTimeMillis() - startTime;

            System.out.println("\n\nTotal distance: " + round(rr.getDistanceTotal()) / 1000 + " km");
            System.out.println("Cars assigned: " + rr.getItineraryQty());
            System.out.println("Calculated in: " + elapsedTime + " ms");


        }


    }
}
