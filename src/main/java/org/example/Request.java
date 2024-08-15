package org.example;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.util.Asserts;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.ChangeValue;
import utils.JsonPathFinder;
import utils.PropertyEnv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import static utils.GlobalData.addKeyValue;
import static utils.GlobalData.getValueByKey;


public class Request {

    static PropertyEnv prop = new PropertyEnv();
    static ChangeValue change = new ChangeValue();
    static JsonPathFinder jsonFinder = new JsonPathFinder();
    static Response response = null;

    public void request(String path) throws Exception {
        RestAssured.reset();
        RequestSpecification request = RestAssured.given().urlEncodingEnabled(false).log().all();
        path = "src/main/resources" + path;
        if (!path.endsWith(".json")) {
            runner(request, path);
        } else {
            String content = new String(Files.readAllBytes(Paths.get(path)));
            if (content.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(content);
                for (Object json : jsonArray) {
                    JSONObject jsonObject = new JSONObject(json.toString());
                    handleRequest(request, jsonObject);
                }
            } else {
                JSONObject jsonObject = new JSONObject(content);
                handleRequest(request, jsonObject);
            }
        }
    }

    private void runner(RequestSpecification request, String folderPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            List<Path> sortedPaths = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
            for (Path path : sortedPaths) {
                processFolder(request, path);
            }
        }
    }

    private static void processFolder(RequestSpecification request, Path path) {

        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            if (content.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(content);
                for (Object jsonString : jsonArray) {
                    JSONObject converter = new JSONObject(jsonString.toString());
                    handleRequest(request, converter);
                }
            } else {
                JSONObject json = new JSONObject(content);
                handleRequest(request, json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleRequest(RequestSpecification request, JSONObject path) throws Exception {
        JSONObject json = null;
        json = change.processJsonPlaceholders(path);
        json = change.replaceFaker(json);

        if (json.has("headers")) {
            String stringHeader = json.get("headers").toString();
            JSONObject headers = new JSONObject(stringHeader);
            headers.keys().forEachRemaining(key -> {
                request.header(key, headers.getString(key));
            });
        } else {
            request.header("Content-Type", "application/json");
        }
        if (json.has("params")) {
            String stringObject = json.get("params").toString();
            JSONObject params = new JSONObject(stringObject);
            params.keys().forEachRemaining(key -> {
                request.param(key, params.getString(key));
            });
        }
        if (json.has("query")) {
            String stringObject = json.get("query").toString();
            JSONObject params = new JSONObject(stringObject);
            params.keys().forEachRemaining(key -> {
                request.queryParam(key, params.getString(key));
            });
        }
        if (json.has("form")) {
            String stringObject = json.get("form").toString();
            JSONObject params = new JSONObject(stringObject);
            params.keys().forEachRemaining(key -> {
                request.formParam(key, params.getString(key));
            });
        }
        if (json.has("path")) {
            String stringObject = json.get("path").toString();
            JSONObject params = new JSONObject(stringObject);
            params.keys().forEachRemaining(key -> {
                request.pathParam(key, params.getString(key));
            });
        }
        if (json.has("body")) {
            String body = json.get("body").toString();
            request.body(body);
        }
        if (json.has("get")) {
            response = request.get(handleURL(json.getString("get")));
            response.then().log().body();
        } else if (json.has("post")) {
            response = request.post(handleURL(json.getString("post")));
            response.then().log().body();
        } else if (json.has("put")) {
            response = request.put(handleURL(json.getString("put")));
            response.then().log().body();
        } else if (json.has("delete")) {
            response = request.delete(handleURL(json.getString("delete")));
            response.then().log().body();
        }
        if (json.has("status")) {
            response.then().statusCode(json.getInt("status"));

        }
        if (json.has("expect")) {
            String expect = json.getString("expect");
            handleExpect(expect);
        }
    }

    private static void handleExpect(String expect) throws Exception {
        System.out.println("\n");

        if (!expect.trim().contains("===") && !expect.contains(":::") && !expect.trim().contains(">")) {
            List<Object> results = jsonFinder.findValuesByPath(response.body().asString(), expect.trim());

            if (results.isEmpty()) {
                throw new Exception("Not found path " + expect.trim());
            }
            // expect
            if (results.size() == 1) {
                expectGreen("Found: " + results.get(0));
            } else {
                expectGreen("Found: " + results);
            }

        } else if (expect.trim().contains("===") && !expect.trim().contains(":::") && !expect.trim().contains(">")) {
            boolean value = false;
            Object[] expectSplit = expect.trim().split("===");
            List<Object> results = jsonFinder.findValuesByPath(response.body().asString(), expectSplit[0]);
            value = results.stream().anyMatch(str -> String.valueOf(str).equals(expectSplit[1]));

            if (!value) {
                throw new Exception("value " + expectSplit[1] + " not found in " + results);
            }
            if (results.size() == 1) {
                expectGreen("Value " + expectSplit[1] + " found in " + results.get(0));
            } else {
                expectGreen("Value " + expectSplit[1] + " found in " + results);
            }


        } else if (expect.trim().contains("===") && expect.trim().contains(":::") && !expect.trim().contains(">")) {
            boolean value = false;
            String[] expectSplit = expect.trim().split("===");
            String[] expectSplitSave = expectSplit[1].split(":::");


            List<Object> results = jsonFinder.findValuesByPath(response.body().asString(), expectSplit[0]);
            value = results.stream().anyMatch(str -> String.valueOf(str).equals(expectSplitSave[0]));

            if (!value) {
                throw new Exception("value " + expectSplitSave[0] + " not found in " + results);
            }
            Object output = results.size() == 1 ? results.get(0) : results;
            expectGreen("Found: " + output);
            addKeyValue(expectSplitSave[1], output);
            expectOrange("path: " + expectSplitSave[1] + " value: " + output);

        } else if (expect.trim().contains("===") && expect.trim().contains(":::") && expect.trim().contains(">")) {
            boolean value = false;
            String[] expectSplit = expect.trim().split("===");
            String[] expectSplitSave = expectSplit[1].trim().split(":::");
            String[] expectSplitIndex = expectSplitSave[1].trim().split(">");

            List<Object> results = jsonFinder.findValuesByPath(response.body().asString(), expectSplit[0]);
            value = results.stream().anyMatch(str -> String.valueOf(str).equals(expectSplitSave[0]));

            if (!value) {
                throw new Exception("value " + expectSplitSave[0] + " not found in " + results);
            }
            Object output = results.size() == 1 ? results.get(0) : results;
            expectGreen("Found: " + results);
            addKeyValue(expectSplitIndex[0], results.get(Integer.valueOf(expectSplitIndex[1]) - 1));
            expectOrange("path " + expectSplitIndex[0] + " value: " + results.get(Integer.valueOf(expectSplitIndex[1]) - 1));

        } else if (!expect.trim().contains("===") && expect.contains(":::") && expect.trim().contains(">")) {
            String[] expectSplit = expect.trim().split(":::");
            String[] expectSplitIndex = expectSplit[1].trim().split(">");

            List<Object> results = jsonFinder.findValuesByPath(response.body().asString(), expectSplit[0]);

            if (results == null) {
                throw new Exception("path " + expectSplit[0] + " not found in values.");
            }
            addKeyValue(expectSplitIndex[0], results.get(Integer.valueOf(expectSplitIndex[1]) - 1));
            expectOrange("path: " + expectSplitIndex[0] + " value: " + results.get(Integer.valueOf(expectSplitIndex[1]) - 1));

        } else if (!expect.trim().contains("===") && expect.contains(":::") && !expect.trim().contains(">")) {
            String[] expectSplit = expect.trim().split(":::");

            List<Object> results = jsonFinder.findValuesByPath(response.body().asString(), expectSplit[0]);

            if (results == null) {
                throw new Exception("path " + expectSplit[0] + " not found in values.");
            }
            if (results.size() == 1) {

                addKeyValue(expectSplit[0].trim(), results.get(0));
                expectOrange("path " + expectSplit[0] + " value: " + results.get(0));

            } else {
                addKeyValue(expectSplit[0].trim(), results);
                expectOrange("path " + expectSplit[0] + " value: " + results);
            }
        }
        System.out.println("\n");
    }

    private static void expectGreen(String text) {
        String ANSI_GREEN = "\u001B[32m";
        String ANSI_RESET = "\u001B[0m";
        System.out.println(ANSI_GREEN + "✅ \uD83C\uDD74\uD83C\uDD87\uD83C\uDD7F\uD83C\uDD74\uD83C\uDD72\uD83C\uDD83  " + ANSI_RESET + text);

    }

    private static void expectOrange(String text) {
        String ANSI_ORANGE = "\u001B[38;5;208m";
        String ANSI_RESET = "\u001B[0m";
        System.out.println(ANSI_ORANGE + "\uD83D\uDCBE \uD83C\uDD82\uD83C\uDD70\uD83C\uDD85\uD83C\uDD74  " + ANSI_RESET + text);
    }

    private static String handleURL(String url) throws IOException {
        String urlFinal = null;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        JSONObject jsonEnv = prop.json(prop.propertyRead("env")).getJSONObject(prop.propertyRead("environment"));
        if (url.contains("/")) {
            String[] parts = url.split("/");
            for (String part : parts) {
                if (jsonEnv.has(part)) {
                    if (urlFinal == null) {
                        urlFinal = jsonEnv.get(part).toString();
                    } else {
                        if (urlFinal.endsWith(("/"))) urlFinal = urlFinal.substring(0, urlFinal.length() - 1);
                        urlFinal += "/" + jsonEnv.get(part).toString();
                    }
                } else {
                    if (urlFinal.endsWith(("/"))) urlFinal = urlFinal.substring(0, urlFinal.length() - 1);
                    urlFinal += "/" + part;
                }
            }
            return urlFinal;
        } else {
            return jsonEnv.getString(url);
        }
    }

}
