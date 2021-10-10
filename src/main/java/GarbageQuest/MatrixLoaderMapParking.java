package GarbageQuest;

import GarbageQuest.entity.*;
import GarbageQuest.mosData.GarbageSiteMD;
import GarbageQuest.mosData.PaidParkingMD;
import GarbageQuest.service.*;
import GarbageQuest.supplimentary.MockTimeSlots;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graphhopper.util.StopWatch;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.round;


public class MatrixLoaderMapParking {
    public static void main(String[] args) {
        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\Users\\User\\Documents\\GD\\MDpark-ALL.json";
        boolean filterByRegion = true; // true for Region, false for AO
        String filter = "район Арбат"; // "Северо-Восточный административный округ", ...


        //OSM data
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";

        String jsonOutputFile1 = "C:\\Users\\User\\Documents\\GD\\arbat-park-good.json";
        String jsonOutputFile2 = "C:\\Users\\User\\Documents\\GD\\arbat-park-bad.json";

        boolean runAlgo = true; // execute itinerary routing algo at this stage?
        //int avgSpeed = 10; // Average video complex car speed in m/s, 10 is roughly 30 km/h
        int maxTime = 900000; // max time for circulation in ms
        int trim = 10000000; // correction value for special cases, don't set low (<10000000) unless smth wrong
        // ----- CONTROLS END ---------



        // ----- IMPORT FROM MOSDATA JSON & FILTER ------
        List<PaidParkingMD> input = MosDataImportParking.Load(jsonInputFile);
        List<WayPoint> wayPointList = new ArrayList<>();
        for (PaidParkingMD pp : input) {
            //System.out.println(pp);
            if ((pp.getDistrict().equals(filter) && filterByRegion)
                    || (pp.getAdmArea().equals(filter) && !filterByRegion)) {
                WayPoint wp = new WayPoint();
                wp.setIndex(pp.getGlobal_id());

                wp.setLat(pp.getGeoData().getCoordinates()[0][0][1]);
                wp.setLon(pp.getGeoData().getCoordinates()[0][0][0]);

                wp.setDescription(pp.getParkingName());

                wp.setType(WayPointType.Paid_Parking);
                wp.setCapacity(pp.getCarCapacity()+pp.getCarCapacityDisabled());

                wayPointList.add(wp);
            }
        }
        System.out.println("\nLoaded " + wayPointList.size() + " sites for " + filter + "\n");


        // ---- CREATE BASE ----

        WayPoint base = new WayPoint(0, 55.73235400160589, 37.54848003387452, "BASE",  // example ))
                LocalTime.parse("06:00"), LocalTime.parse("08:00"), Duration.ofMinutes(1),
                WayPointType.Base, 1);


        // ------ NOW FILL MATRICES -----

        StopWatch sw = new StopWatch().start();
        DoubleMatrix doubleMatrix = Matrix.FillGHDoubleTime(
                wayPointList,osmFile,dir,base);

        System.out.println("\nMatrix calculated in: " + sw.stop().getSeconds() + " s\n");

        // ----- AND SAVE MATRICES IN JSON -----
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        if(doubleMatrix.getMapGood().size()>0) {
            try (FileWriter writer = new FileWriter(jsonOutputFile1)) {
                for (WayPoint wp : doubleMatrix.getWayPointsGood()) {
                    MatrixStorageLine msl = new MatrixStorageLine();
                    msl.setWayPoint(wp);

                    List<Double> dd = new ArrayList<>();
                    for (WayPoint wwp : doubleMatrix.getWayPointsGood()) {
                        dd.add(doubleMatrix.getMapGood().get(wp).getDistances().get(wwp));
                    }

                    msl.setDistances(dd);

                    writer.write(objectMapper.writeValueAsString(msl));
                    writer.write("\n");
                }
                writer.flush();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }

            System.out.println("Saved as: " + jsonOutputFile1);
        }

        if(doubleMatrix.getMapBad().size()>0) {
            try (FileWriter writer = new FileWriter(jsonOutputFile2)) {
                for (WayPoint wp : doubleMatrix.getWayPointsBad()) {
                    MatrixStorageLine msl = new MatrixStorageLine();
                    msl.setWayPoint(wp);

                    List<Double> dd = new ArrayList<>();
                    for (WayPoint wwp : doubleMatrix.getWayPointsBad()) {
                        dd.add(doubleMatrix.getMapBad().get(wp).getDistances().get(wwp));
                    }

                    msl.setDistances(dd);

                    writer.write(objectMapper.writeValueAsString(msl));
                    writer.write("\n");
                }
                writer.flush();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }

            System.out.println("Saved as: " + jsonOutputFile2);
        }


        // ----- EXECUTE ITINERARY ALGORITHM IF DESIRED -----
        if(runAlgo) {
            if (doubleMatrix.getMapGood().size() > 0) {
                long startTime = System.currentTimeMillis();
                Result rr = AlgCircularMatrixMapV01.Calculate(
                        doubleMatrix.getWayPointsGood(), doubleMatrix.getMapGood(), maxTime, trim);
                long elapsedTime = System.currentTimeMillis() - startTime;

                System.out.println("\n\nTotal distance: " + round(rr.getDistanceTotal()) / 1000 + " km");
                System.out.println("Cars assigned: " + rr.getItineraryQty());
                System.out.println("Calculated in: " + elapsedTime + " ms");
            }

            if (doubleMatrix.getMapBad().size() > 0) {
                long startTime = System.currentTimeMillis();
                Result rr = AlgCircularMatrixMapV01.Calculate(
                        doubleMatrix.getWayPointsBad(), doubleMatrix.getMapBad(), maxTime, trim);
                long elapsedTime = System.currentTimeMillis() - startTime;

                System.out.println("\n\nTotal distance: " + round(rr.getDistanceTotal()) / 1000 + " km");
                System.out.println("Cars assigned: " + rr.getItineraryQty());
                System.out.println("Calculated in: " + elapsedTime + " ms");
            }

        }

    }
}
