import GarbageQuest.mosData.GarbageSiteMD;
import GarbageQuest.service.MosDataImportGarbage;
import com.graphhopper.util.StopWatch;
import org.junit.Test;

import java.util.List;

public class LoadMDjson {
    @Test
    public void PrintSomeFields()
    {
        StopWatch sw = new StopWatch().start();
        List<GarbageSiteMD> res = MosDataImportGarbage.Load("C:\\Users\\User\\Documents\\MDjson-ALL.json");
        System.out.println("\nCalculated in: " + sw.stop().getSeconds() + " s\n");

        for (int i=100; i<110; i++)
        {
            System.out.println("\nПомойка: " + res.get(i).getYardName());
            System.out.println("Округ: " + res.get(i).getYardLocation()[0].getAdmArea());
            System.out.println("Район: " + res.get(i).getYardLocation()[0].getDistrict());
            System.out.println("Геокоординаты: " + res.get(i).getGeoData().getCoordinates()[0] + "," + res.get(i).getGeoData().getCoordinates()[1]);
        }

        System.out.println("\nВсего: " + res.size() + " помоек");
    }
}
