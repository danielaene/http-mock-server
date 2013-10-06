import fi.iki.elonen.Method;
import junit.framework.Assert;
import fi.iki.elonen.Status;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.idev.tools.hms.mock.MockServer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MockServerTest {
    private static String SERVER_ADDRESS = "127.0.0.1";
    private static int SERVER_PORT = 9191;
    private static MockServer server;

    @BeforeClass
    public static void before() throws IOException {
        server = new MockServer(SERVER_ADDRESS, SERVER_PORT);
        server.start();
    }

    @AfterClass
    public static void after() {
        server = null;
    }

    @Test
    public void testStart2ServersSameAddress() throws IOException {
        String responseBody = "Here goes the response body";
        server.when("/server/1", Method.GET).thenReturn(responseBody);
        server.when("/server/2", Method.GET).thenReturn(Status.BAD_REQUEST);

        HttpURLConnection con = makeRestCall("http://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/server/1");
        Assert.assertEquals(200, con.getResponseCode());
        Assert.assertEquals(responseBody, readResponseBody(con));

        HttpURLConnection con2 = makeRestCall("http://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/server/2");
        Assert.assertEquals(400, con2.getResponseCode());

        HttpURLConnection con3 = makeRestCall("http://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/server/3");
        Assert.assertEquals(404, con3.getResponseCode());
    }

    public HttpURLConnection makeRestCall(String uri) throws IOException {
        URL url = new URL(uri);
        String query = "";

        //make connection
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
//        con.setRequestProperty("Content-Type", "application/xml");
        //use post mode
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.connect();
        return con;
    }

    public String readResponseBody(HttpURLConnection con) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(con
                    .getInputStream()));
            String l = null;
            StringBuilder sb = new StringBuilder();
            while ((l = br.readLine()) != null) {
                sb.append(l);
            }
            br.close();
            return sb.toString();
        } catch (FileNotFoundException e) {
            //no response body found
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
