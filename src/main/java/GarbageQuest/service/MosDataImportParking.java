package GarbageQuest.service;

import GarbageQuest.entity.WayPoint;
import GarbageQuest.mosData.GarbageSiteMD;
import GarbageQuest.mosData.PaidParkingMD;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MosDataImportParking {
    public static List<PaidParkingMD> Load(String filename)
    {

        List<PaidParkingMD> input = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // skip unused fields
        String inString = null;
        try {
            inString = new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(inString);
        try {
            input = objectMapper.readValue(inString, new TypeReference<List<PaidParkingMD>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }



        return input;
    }
}
