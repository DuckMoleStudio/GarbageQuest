import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegExGames {
    @Test
    public void RegEx1()
    {
        String inString = "POINT (37.5378776700138 55.7062823426552)";
        String[] digits = inString.split("\\D+");
        System.out.println("0: " + digits[0]);
        System.out.println("1: " + digits[1]);
        List<String> out = Arrays.asList(digits);
        System.out.println("size: " + out.size());
        for(String ss: out)
            System.out.println(ss);
    }

    @Test
    public void RegEx2()
    {
        String inString = "POINT (37.5378776700138 55.7062823426552)";
        String[] digits = inString.split("[^[0-9.]]+");
        System.out.println("0: " + digits[1]);
        System.out.println("1: " + digits[2]);
        List<String> out = Arrays.asList(digits);
        System.out.println("size: " + out.size());
        for(String ss: out)
            System.out.println(ss);
    }
}
