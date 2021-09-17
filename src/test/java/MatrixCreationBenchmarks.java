import GarbageQuest.entity.MatrixLine;
import GarbageQuest.entity.WayPoint;
import GarbageQuest.service.Matrix;
import GarbageQuest.supplimentary.MockInput;
import com.graphhopper.util.StopWatch;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MatrixCreationBenchmarks {
    @Test
    public void VeryFirst()
    {
        List<WayPoint> wayPointsCommon = MockInput.randomWP();

        StopWatch sw = new StopWatch().start();
        List<MatrixLine> res1 = Matrix.FillGHSingle(wayPointsCommon);
        System.out.println("\nCalculated in: " + sw.stop().getSeconds() + " s\n");
        sw.start();
        List<MatrixLine> res2 = Matrix.FillGHMulti4(wayPointsCommon,"C:/Users/User/Downloads/RU-MOW.osm.pbf","local/graphhopper");
        System.out.println("\nCalculated in: " + sw.stop().getSeconds() + " s\n");
        assertEquals (res1.size(), res2.size());

        for(WayPoint wp1 : wayPointsCommon)
        {
            for(WayPoint wp2 : wayPointsCommon)
            {
                assertEquals(Matrix.DistanceBetween(wp1,wp2,res1),Matrix.DistanceBetween(wp1,wp2,res2),0.01);
            }
        }
    }
}
