import GarbageQuest.entity.MatrixLineMap;
import GarbageQuest.entity.MatrixStorageLine;
import GarbageQuest.entity.WayPoint;
import GarbageQuest.service.Matrix;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GHRouteCheck {
    @Test
    public void IfSumEqualsVia()
    {
        // ------- RESTORE MATRIX FROM JSON FILE ---------

        String jsonInputFile = "C:\\Users\\User\\Documents\\GD\\hamovniki-park-good.json"; // should exist!
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

        String urlOutputFile = "C:\\Users\\User\\Documents\\GD\\hamovniki-park-test.txt";
        try (FileWriter writer = new FileWriter(urlOutputFile)) {
            double totalDistance = 0;
            String url = "https://graphhopper.com/maps/?";

            for (int i = 0; i < 10; i++) {
                double newDistance = Matrix.DistanceBetweenMap(
                        wayPointList.get(i),
                        wayPointList.get(i + 1),
                        matrix);
                writer.write("\ndist " + i + ": " + newDistance);
                totalDistance += newDistance;

                url += "point=";
                url += wayPointList.get(i).getLat();
                url += "%2C";
                url += wayPointList.get(i).getLon();
                url += "&";
            }
            //for (int i = 0; i < 10; i++)
            //{url += "curbside=right&";}

            writer.write("\n\nTotal dist: " + totalDistance/60000);
            url+="locale=ru-RU&profile=car&use_miles=false&turn_costs=true&u_turn_costs=80";
            writer.write("\n\n");
            writer.write(url);
            writer.write("\n\n");

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
