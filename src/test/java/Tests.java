import org.example.Request;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Tests extends Request {


    @Test()
    public void crud_get() throws Exception {
        request("/crud");
    }
}
