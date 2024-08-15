package utils;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertyEnv {

    public JSONObject json(String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        JSONObject json = new JSONObject(content);
        return json;
    }

    public String propertyRead(String props) throws IOException {
        Properties prop = new Properties();
        InputStream input = new FileInputStream("src/main/resources/config.properties");
        prop.load(input);
        return prop.getProperty(props);
    }
}
