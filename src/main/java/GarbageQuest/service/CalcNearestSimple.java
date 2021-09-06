package GarbageQuest.service;

import GarbageQuest.entity.WayPoint;

import java.util.List;

public class CalcNearestSimple {
    public static WayPoint calc(WayPoint start, List<WayPoint> wayPointList)
    {

        double minDistance = CalcDistanceSimple.Calc(start.getLat(),start.getLon(),
                wayPointList.get(0).getLat(),wayPointList.get(0).getLon());
        WayPoint result = wayPointList.get(0);
        for(WayPoint ww: wayPointList)
        {
            if(CalcDistanceSimple.Calc(start.getLat(),start.getLon(),ww.getLat(),ww.getLon()) < minDistance)
            {
                minDistance = CalcDistanceSimple.Calc(start.getLat(),start.getLon(),ww.getLat(),ww.getLon());
                result = ww;
            }
        }

        return result;
    }
}
