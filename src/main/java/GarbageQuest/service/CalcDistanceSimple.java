package GarbageQuest.service;

public class CalcDistanceSimple {
    public static double CALL_COUNT=0;

    public static double Calc(double lat1, double lon1, double lat2, double lon2)
    {
        CALL_COUNT++;
        return Math.abs((lat1-lat2)*142860)+Math.abs((lon1-lon2)*58825); // empiric values for Moscow latitude
    }
}
