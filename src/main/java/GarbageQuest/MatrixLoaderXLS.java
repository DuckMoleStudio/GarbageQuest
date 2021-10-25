package GarbageQuest;

import GarbageQuest.entity.*;
import GarbageQuest.mosData.XLSPaidParkings;
import GarbageQuest.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graphhopper.util.StopWatch;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.round;


public class MatrixLoaderXLS {
    public static void main(String[] args) throws IOException {

        // ----- CONTROLS (set before use) -----------
        String XLSInputFile1 = "C:\\Users\\User\\Documents\\GD\\park_all.xls";
        String XLSInputFile2 = "C:\\Users\\User\\Documents\\GD\\signs_all.xls";
        boolean filterByRegion = true; // true for Region, false for AO
        String filter = "муниципальный округ Арбат"; // "Северо-Восточный административный округ", ...
        String filter1 = "муниципальный округ Хамовники"; // "Северо-Восточный административный округ", ...

        //OSM data
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";

        String jsonOutputFile1 = "C:\\Users\\User\\Documents\\GD\\ah-park-good.json";
        String jsonOutputFile2 = "C:\\Users\\User\\Documents\\GD\\ah-park-bad.json";

        boolean runAlgo = true; // execute itinerary routing algo at this stage?

        int maxTime = 600000; // max time for circulation in ms
        int trim = 10000000; // correction value for special cases, don't set low (<10000000) unless smth wrong
        // ----- CONTROLS END ---------



        // ----- IMPORT FROM XLS & PARSE ------
        StopWatch sw = new StopWatch().start();
        List<XLSPaidParkings> input = XLSImport.loadXLSPaidParkings(XLSInputFile1);
        System.out.println("XLS parkings loaded in: " + sw.stop().getSeconds() + " s");


        sw = new StopWatch().start();
        input.addAll(XLSImport.loadXLSPaidParkings(XLSInputFile2));
        System.out.println("XLS signs loaded in: " + sw.stop().getSeconds() + " s");



        List<WayPoint> wayPointList = new ArrayList<>();
        for (XLSPaidParkings pp : input)
            if (((pp.getDistrict().equals(filter)||pp.getDistrict().equals(filter1)) && filterByRegion)
                    || (pp.getAdmArea().equals(filter) && !filterByRegion))
        {
            WayPoint wp = new WayPoint();
            wp.setIndex(pp.getGlobal_id());
            wp.setDescription(pp.getAddress());

            String[] digits = pp.getCoordinates().split("[^[0-9.]]+"); // magic regex, see test

            wp.setLat(Double.parseDouble(digits[2]));
            wp.setLon(Double.parseDouble(digits[1]));

            wp.setType(WayPointType.Paid_Parking);
            wp.setCapacity(1);
            wayPointList.add(wp);
            }

        System.out.println("\nLoaded " + wayPointList.size() + " points for " + filter);

        // ---- CREATE BASE ----

        WayPoint base = new WayPoint(0, 55.75468, 37.69016, "BASE",  // ул. Золоторожский Вал, 4А ))
                LocalTime.parse("06:00"), LocalTime.parse("08:00"), Duration.ofMinutes(1),
                WayPointType.Base, 1);


        // ------ NOW FILL MATRICES -----

        sw = new StopWatch().start();
        DoubleMatrix doubleMatrix = Matrix.FillGHDoubleTime(
                wayPointList,osmFile,dir,base);
       // Map<WayPoint,MatrixLineMap> matrix = Matrix.FillGHMulti4Map(
        //        wayPointList, osmFile, dir, true, true);
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

            System.out.println("Saved as: " + jsonOutputFile2 + "\n");
        }


        // ----- EXECUTE ITINERARY ALGORITHM IF DESIRED -----
        if(runAlgo)
        {
            if(doubleMatrix.getMapGood().size()>0) {
                long startTime = System.currentTimeMillis();
                Result rr = AlgCircularMatrixMapV01.Calculate(
                        doubleMatrix.getWayPointsGood(), doubleMatrix.getMapGood(), maxTime, trim);
                long elapsedTime = System.currentTimeMillis() - startTime;

                System.out.println("\n\nTotal time: " + round(rr.getDistanceTotal()) / 1000 + " sec");
                System.out.println("Cars assigned: " + rr.getItineraryQty());
                System.out.println("Calculated in: " + elapsedTime + " ms\n");
            }

            if(doubleMatrix.getMapBad().size()>0) {
                long startTime = System.currentTimeMillis();
                Result rr = AlgCircularMatrixMapV01.Calculate(
                        doubleMatrix.getWayPointsBad(), doubleMatrix.getMapBad(), maxTime, trim);
                long elapsedTime = System.currentTimeMillis() - startTime;

                System.out.println("\n\nTotal time: " + round(rr.getDistanceTotal()) / 1000 + " sec");
                System.out.println("Cars assigned: " + rr.getItineraryQty());
                System.out.println("Calculated in: " + elapsedTime + " ms\n");
            }


        }
    }
}
