import GarbageQuest.entity.WayPoint;
import GarbageQuest.entity.WayPointType;
import GarbageQuest.mosData.XLSPaidParkings;
import GarbageQuest.service.XLSImport;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WeirdXLS {
    @Test
    public void LoadPark()
    {
        String XLSInputFile = "C:\\Users\\User\\Documents\\GD\\ramenki_park.xls";
        List<XLSPaidParkings> input = null;
        try {
            input = XLSImport.loadXLSPaidParkings(XLSInputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (XLSPaidParkings pp : input)
        {
            String[] digits = pp.getCoordinates().split("[^[0-9.]]+"); // magic regex, see test

            System.out.println(
                    "\nid: " + pp.getGlobal_id() +
                    " desc: " + pp.getAddress() +
                    " coord: " + digits[2] + " " + digits[1]);
        }

        System.out.println("\nLoaded " + input.size() + " sites\n");
    }
}
