package GarbageQuest.supplimentary;

import GarbageQuest.entity.WayPoint;
import GarbageQuest.entity.WayPointType;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockInput {

    public static List<WayPoint> randomWP()
    {
        List<WayPoint> wayPoints = new ArrayList<>();
        // --- CONTROLS START ---
        int qty = 100; // how many
        double latStart = 55.65; // more or less MSU region
        double latEnd = 55.7;
        double lonStart = 37.5;
        double lonEnd = 37.6;
        DistType lonDist = DistType.Equal; // distribution: equal, gaussian, (pseudo)expo ascending or descending
        DistType latDist = DistType.Gauss;

        int timeStartMin = 6; // opening time, hours in 24h
        int timeStartMax = 12; // non-inclusive
        DistType timeDist = DistType.Descend;

        int intervalMin = 1; // from open to close, hours
        int intervalMax = 8; // non-inclusive
        DistType intervalDist = DistType.Ascend;
        // --- CONTROLS END ---

        // BASE & DUMP MANUALLY
        wayPoints.add(new WayPoint(55.7, 37.5, "BASE",
                LocalTime.parse("06:00"), LocalTime.parse("08:00"), Duration.ofMinutes(1),
                WayPointType.Base, 1));
        wayPoints.add(new WayPoint(55.7, 37.6, "DUMP SITE",
                LocalTime.parse("06:00"), LocalTime.parse("08:00"), Duration.ofMinutes(40),
                WayPointType.Garbage_Dump, 1));

        Random random = new Random();
        for(int i=0; i < qty; i++)
        {
            double lon = 0;
            switch (lonDist)
            {
                case Equal:
                    lon = random.nextDouble()*(lonEnd-lonStart)+lonStart;
                    break;

                case Gauss:
                    lon = RandomMethods.truncGauss()*(lonEnd-lonStart)+lonStart;
                    break;

                case Ascend:
                    lon = RandomMethods.expoAsc()*(lonEnd-lonStart)+lonStart;
                    break;

                case Descend:
                    lon = RandomMethods.expoDesc()*(lonEnd-lonStart)+lonStart;
                    break;

                default:
                    break;
            }

            double lat = 0;
            switch (latDist)
            {
                case Equal:
                    lat = random.nextDouble()*(latEnd-latStart)+latStart;
                    break;

                case Gauss:
                    lat = RandomMethods.truncGauss()*(latEnd-latStart)+latStart;
                    break;

                case Ascend:
                    lat = RandomMethods.expoAsc()*(latEnd-latStart)+latStart;
                    break;

                case Descend:
                    lat = RandomMethods.expoDesc()*(latEnd-latStart)+latStart;
                    break;

                default:
                    break;
            }

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

            String desc = "Garbage site # "+(i+1);
            wayPoints.add(new WayPoint(lat, lon, desc, startTime, endTime, Duration.ofMinutes(10),
                    WayPointType.Garbage_Site, 1));

        }

        return wayPoints;
    }
}
