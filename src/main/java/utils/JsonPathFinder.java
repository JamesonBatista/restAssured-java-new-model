package utils;

import org.json.*;

import java.util.ArrayList;
import java.util.List;

public class JsonPathFinder {
    public List<Object> findValuesByPath(String jsonString, Object path) {
        List<Object> results = new ArrayList<>();
        Object json = jsonString.trim().startsWith("{") ? new JSONObject(jsonString) : new JSONArray(jsonString);
        findValuesByPathRecursive(json, path, results);
        return results;
    }

    private void findValuesByPathRecursive(Object json, Object path, List<Object> results) {
        if (json instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) json;
            for (String key : jsonObject.keySet()) {
                Object value = jsonObject.get(key);
                if (key.equals(path)) {
                    results.add(value);
                }
                findValuesByPathRecursive(value, path, results);
            }
        } else if (json instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) json;
            for (int i = 0; i < jsonArray.length(); i++) {
                Object value = jsonArray.get(i);
                findValuesByPathRecursive(value, path, results);
            }
        }
    }
}
