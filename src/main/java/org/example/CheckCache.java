package org.example;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CheckCache {
    // Set the root directory
    public static String rootDir = "C:\\Users\\os-sajal.halder\\Desktop\\ECSG\\gateway\\japan.gateway\\src\\main\\resources\\reloadable.config";
    // Set the checkFile name
    public static String checkFileName = "settings-prod.json";

    public static void main(String[] args) {


        // Lists to store enabled and disabled directories
        List<String> enabledDirectories = new ArrayList<>();
        List<String> disabledDirectories = new ArrayList<>();
        List<String> allFieldCacheKey = new ArrayList<>();
        List<String> notAllFieldCacheKey = new ArrayList<>();

        // Create an ExecutorService with a fixed number of threads
        int numThreads = Runtime.getRuntime().availableProcessors(); // Use the number of available processors
        try (ExecutorService executorService = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("cache-enable-checker-vt", 0).factory())) {

            // List to store Future tasks
            List<Future<?>> futures = new ArrayList<>();

            // Loop through directories
            for (String dir : Objects.requireNonNull(new File(rootDir).list())) {
                String jsonFilePath = rootDir + "\\" + dir + "\\" + checkFileName;

                // Submit each directory for processing
                futures.add(executorService.submit(
                        () -> processDirectory(jsonFilePath, dir, enabledDirectories, disabledDirectories,
                                allFieldCacheKey,notAllFieldCacheKey)));
            }

            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Shut down the ExecutorService
            executorService.shutdown();
        }

        // Display the results

        printDirectories("AllCache", allFieldCacheKey);
        printDirectories("NotAllCache", notAllFieldCacheKey);
//        printDirectories("Enabled", enabledDirectories);
        printDirectories("Disabled", disabledDirectories);
//        printDirectories("Disabled", disabledDirectories);
    }

    private static void processDirectory(String jsonFilePath, String dir, List<String> enabledDirectories,
                                         List<String> disabledDirectories,List<String> allFields,
                                         List<String> notAllField) {
        // Check if settings-prod.json exists in the current directory
        if (new File(jsonFilePath).exists()) {
            try {
                if (isCacheEnabled(jsonFilePath)) {
                    enabledDirectories.add(dir);
                    if(hasCacheKeyForAllParameter(jsonFilePath)){
                        allFields.add(dir);
                    } else{
                        notAllField.add(dir);
                    }
                } else {
                    disabledDirectories.add(dir);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static JSONObject getJsonObject(String jsonFilePath) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(jsonFilePath));
        return (JSONObject) obj;
    }

    private static void printDirectories(String status, List<String> directories) {
        System.out.format("%-15s | %s%n", status, "Directories");
        System.out.println("-------------------------");

        for (String directory : directories) {
            System.out.format("%-15s | %s%n", "", directory);
        }
        System.out.println();
    }

    private static boolean isCacheEnabled(String jsonFilePath) throws IOException, ParseException {
        JSONObject jsonObject = getJsonObject(jsonFilePath);
        Object cache = jsonObject.get("cache");
        return cache instanceof JSONObject &&
                "true".equalsIgnoreCase(((JSONObject) cache).get("enabled").toString());
    }

    private static boolean hasCacheKeyForAllParameter(String directoryPath) throws IOException, ParseException {
        String schemaFilePath = directoryPath + "\\..\\schema.json";
        if (new File(schemaFilePath).exists()) {
            JSONObject jsonObject = getJsonObject(schemaFilePath);
            JSONObject requestObj = (JSONObject) jsonObject.get("request");
            return checkCacheKeyParameters(requestObj,"urlParameters") &&
                    checkCacheKeyParameters(requestObj,"metaParameters") &&
                    checkCacheKeyParameters(requestObj,"bodyParameters");
        }
        return false;
    }
    private static boolean checkCacheKeyParameters(JSONObject requestObj,String parameterName) {
        JSONArray jsonArray = (JSONArray) requestObj.get(parameterName);
        if(jsonArray == null || jsonArray.isEmpty()){
            return true;
        }
        for (Object obj : jsonArray) {
            if (obj instanceof JSONObject) {
                Object cacheKeyObj = ((JSONObject) obj).get("cacheKey");
                if (cacheKeyObj == null) {
                    return false;
                }
            }
        }
        return true; // All objects have "cacheKey"
    }
}

