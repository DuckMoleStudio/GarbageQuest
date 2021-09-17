package GarbageQuest.supplimentary;

import GarbageQuest.entity.WayPoint;
import GarbageQuest.entity.WayPointType;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockTimeSlots {
    public static void fill(List<WayPoint> wayPoints,
                            int timeStartMin,
                            int timeStartMax,
                            DistType timeDist,
                            int intervalMin,
                            int intervalMax,
                            DistType intervalDist)
    {
        Random random = new Random();
        for(WayPoint wp: wayPoints)
        {
        LocalTime startTime = LocalTime.parse("00:00");
            switch (timeDist)
            {
                case Equal:
                    startTime = LocalTime.of
                            (random.nextInt(timeStartMax-timeStartMin)+timeStartMin,0);
                    break;

                case Gauss:
                    startTime = LocalTime.of
                            ((int)(RandomMethods.truncGauss()*(timeStartMax-timeStartMin))+timeStartMin,0);
                    break;

                case Ascend:
                    startTime = LocalTime.of
                            ((int)(RandomMethods.expoAsc()*(timeStartMax-timeStartMin))+timeStartMin,0);
                    break;

                case Descend:
                    startTime = LocalTime.of
                            ((int)(RandomMethods.expoDesc()*(timeStartMax-timeStartMin))+timeStartMin,0);
                    break;

                default:
                    break;
            }

            LocalTime endTime = LocalTime.parse("00:00");
            switch (intervalDist)
            {
                case Equal:
                    endTime = startTime.plus(Duration.ofHours
                            (random.nextInt(intervalMax-intervalMin)+intervalMin));
                    break;

                case Gauss:
                    endTime = startTime.plus(Duration.ofHours
                            ((int)(RandomMethods.truncGauss()*(intervalMax-intervalMin)+intervalMin)));
                    break;

                case Ascend:
                    endTime = startTime.plus(Duration.ofHours
                            ((int)(RandomMethods.expoAsc()*(intervalMax-intervalMin)+intervalMin)));
                    break;

                case Descend:
                    endTime = startTime.plus(Duration.ofHours
                            ((int)(RandomMethods.expoDesc()*(intervalMax-intervalMin)+intervalMin)));
                    break;

                default:
                    break;
            }
            wp.setTimeOpen(startTime);
            wp.setTimeClose(endTime);
        }
    }
}
