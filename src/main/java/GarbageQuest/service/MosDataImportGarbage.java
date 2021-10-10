package GarbageQuest.service;

import GarbageQuest.entity.WayPoint;
import GarbageQuest.mosData.GarbageSiteMD;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MosDataImportGarbage {
    public static List<GarbageSiteMD> Load(String filename)
    {

        List<GarbageSiteMD> input = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String inString = null;
        try {
            inString = new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(inString);
        try {
            input = objectMapper.readValue(inString, new TypeReference<List<GarbageSiteMD>>(){});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }



        return input;
    }
}
