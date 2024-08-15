package utils;

import java.util.ArrayList;
import java.util.List;

public class GlobalData {
    private static List<KeyValue> keyValueList = new ArrayList<>();

    public static void addKeyValue(String key, Object value) {
        keyValueList.add(new KeyValue(key, value));
    }

    public static Object getValueByKey(String key) {
        if (key == null) {
            keyValueList.forEach((item) -> {
                System.out.println("save: " + item.key);
                System.out.println("save: " + item.value);

            });
            System.out.println(keyValueList);
        }
        for (KeyValue keyValue : keyValueList) {
            if (keyValue.getKey().equals(key)) {
                return keyValue.getValue();
            }
        }
        return null; // Retorna null se a chave não for encontrada
    }

    private static class KeyValue {
        private String key;
        private Object value;

        public KeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }
}
