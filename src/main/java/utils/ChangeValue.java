package utils;

import com.github.javafaker.Faker;
import org.json.JSONObject;

import java.util.Locale;

import static utils.GlobalData.addKeyValue;
import static utils.GlobalData.getValueByKey;

public class ChangeValue {
    static Faker faker = new Faker(new Locale("pt-BR"));

    public static JSONObject processJsonPlaceholders(JSONObject json) {
        for (String key : json.keySet()) {
            Object value = json.get(key);
            if (value instanceof String) {
                String strValue = (String) value;
                json.put(key, replacePlaceholders(strValue));
            } else if (value instanceof JSONObject) {
                json.put(key, processJsonPlaceholders((JSONObject) value));
            }
        }
        return json;
    }

    private static String replacePlaceholders(String strValue) {
        StringBuilder result = new StringBuilder();
        int start = 0;
        int openBrace;
        while ((openBrace = strValue.indexOf('{', start)) != -1) {
            int closeBrace = strValue.indexOf('}', openBrace);
            if (closeBrace == -1) break;
            String placeholder = strValue.substring(openBrace + 1, closeBrace);
            Object replacement = getValueByKey(placeholder.trim());
            result.append(strValue, start, openBrace);
            if (replacement != null) {
                result.append(replacement);
            } else {
                result.append(strValue, openBrace, closeBrace + 1);
            }
            start = closeBrace + 1;
        }
        result.append(strValue, start, strValue.length());
        return result.toString();
    }

    public static JSONObject replaceFaker(JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            if (value instanceof JSONObject) {
                replaceFaker((JSONObject) value);
            } else if ("faker.name".equals(value)) {
                Object name = faker.name().fullName();
                jsonObject.put(key.trim(), name);
                addKeyValue(key.trim(), name);
            } else if ("faker.address".equals(value)) {
                Object name = faker.address().streetAddress();
                jsonObject.put(key.trim(), name);
                addKeyValue(key.trim(), name);
            } else if ("faker.email".equals(value)) {
                Object name = faker.internet().emailAddress();
                jsonObject.put(key.trim(), name);
                addKeyValue(key.trim(), name);
            }
        }
        return jsonObject;
    }

}
