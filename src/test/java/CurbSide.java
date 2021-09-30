import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;
import org.junit.Test;

import java.util.Arrays;

public class CurbSide {
    @Test
    public void WithAndWithout()
    {
        GraphHopper hopper = new GraphHopperOSM().forServer();

        //hopper.setEncodingManager(EncodingManager.create("car"));
        hopper.setEncodingManager(EncodingManager.create("car|turn_costs=true"));
        hopper.setGraphHopperLocation("local/graphhopper");
        hopper.setDataReaderFile("C:/Users/User/Downloads/RU-MOW.osm.pbf");

        // see docs/core/profiles.md to learn more about profiles
        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("shortest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("shortest").setTurnCosts(true)
        );

        // this enables speed mode for the profile we call "car" here
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"),new CHProfile("car2"));

        hopper.importOrLoad();

        //GHPoint start = new GHPoint(55.730554,37.564069);
        //GHPoint end = new GHPoint(55.730983,37.564605);

        GHPoint start = new GHPoint(55.7062823426552,37.5378776700138);
        GHPoint end = new GHPoint(55.7076673896416, 37.5403993259513);

        // 1. Our standard way

        GHRequest req1 = new GHRequest()
                .addPoint(start)
                .addPoint(end)
                .setProfile("car1")
                .setAlgorithm(Parameters.Algorithms.ASTAR_BI);
        req1.putHint("instructions", false);
        req1.putHint("calc_points", false);

        GHResponse res1 = hopper.route(req1);
        if (res1.hasErrors())
            throw new RuntimeException(res1.getErrors().toString());
        double distance1 = res1.getBest().getDistance();

        // 1. With curbsides

        GHRequest req2 = new GHRequest()
                .addPoint(start)
                .addPoint(end)
                .setCurbsides(Arrays.asList("right", "right"))
                .setProfile("car2")
                .setAlgorithm(Parameters.Algorithms.ASTAR_BI);
        req2.putHint("instructions", false);
        req2.putHint("calc_points", false);
        req2.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

        GHResponse res2 = hopper.route(req2);
        if (res2.hasErrors())
            throw new RuntimeException(res2.getErrors().toString());
        double distance2 = res2.getBest().getDistance();

        System.out.println("D1: " + distance1 + " D2: " + distance2);
    }
}
