import GarbageQuest.entity.MatrixLineMap;
import GarbageQuest.entity.MatrixStorageLine;
import GarbageQuest.entity.WayPoint;
import GarbageQuest.service.Matrix;
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
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GPXGames {
    @Test
    public void TryToExport()
    {
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";

        /*
        GraphHopper hopper = new GraphHopperOSM().forServer();

        hopper.setEncodingManager(EncodingManager.create("car|turn_costs=true"));
        hopper.setGraphHopperLocation(dir);
        hopper.setDataReaderFile(osmFile);

         */

        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile("C:/Users/User/Downloads/RU-MOW.osm.pbf");
        hopper.setGraphHopperLocation("local/graphhopper");

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("shortest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );

        // this enables speed mode for the profile we call "car" here
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"),new CHProfile("car2"));

        hopper.importOrLoad();

        GHRequest req = new GHRequest(
               55.79
                , 37.579
                , 55.769
                , 37.599)
                .addPoint(new GHPoint(55.74, 37.58))
                .addPoint(new GHPoint(55.73, 37.57))
                .setProfile("car2")
                .setAlgorithm(Parameters.Algorithms.ASTAR_BI);

        //req.putHint("instructions", false);
        //req.putHint("calc_points", false);
        //req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

        GHResponse res = hopper.route(req);

        if (res.hasErrors()) {
             throw new RuntimeException(res.getErrors().toString());
        }

            String gpx = GpxConversions.createGPX(
                    res.getBest().getInstructions(),
                    "some track",
                    0,
                    false,
                    true,
                    true,
                    true,
                    "version",
                    res.getBest().getInstructions().getTr());

            System.out.println(gpx);


        String GPXOutputFile = "C:\\Users\\User\\Documents\\GD\\tracks\\test1.gpx";
        //https://nakarte.me/#m=12/55.74847/37.55573&l=O&nktu=file%3A%2F%2FC%3A%2FUsers%2FUser%2FDocuments%2FGD%2Ftraks%2Ftest2.gpx

        try (FileWriter writer = new FileWriter(GPXOutputFile)) {

            writer.write(gpx);
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }




    }
}
