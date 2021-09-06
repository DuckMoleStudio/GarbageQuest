package GarbageQuest.supplimentary;

import java.util.Random;

import static java.lang.Math.abs;

// methods for special random distributions
public class RandomMethods {

    // gauss truncated to 0-1 peak at 0.5
    public static double truncGauss()
    {
        Random random = new Random();
        double result;
        do
        {result = random.nextGaussian();}
        while (result > 2 || result < -2); // empiric values

        return (result+2)/4;
    }

    // ascending pseudo exponential
    public static double expoAsc()
    {
        Random random = new Random();
        double result;
        do
        {result = random.nextGaussian();}
        while (result > 4 || result < -4); // empiric values

        return (4-abs(result))/4;
    }

    // descending pseudo exponential
    public static double expoDesc()
    {
        Random random = new Random();
        double result;
        do
        {result = random.nextGaussian();}
        while (result > 4 || result < -4); // empiric values

        return abs(result)/4;
    }
}
