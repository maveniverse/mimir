package eu.maveniverse.maven.mimir.node.daemon.protocol;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ProtocolTest {
    @Test
    void smoke() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0);
                Socket clientSck = new Socket("localhost", serverSocket.getLocalPort());
                Socket serverSck = serverSocket.accept()) {
            Thread client = new Thread(
                    () -> {
                        try {
                            Handle handle = new Handle(clientSck.getOutputStream(), clientSck.getInputStream());
                            handle.writeRequest(Request.hello(Map.of("hello", "world")));
                            Response response = handle.readResponse();
                            System.out.println(response);
                        } catch (IOException e) {
                            fail(e);
                        }
                    },
                    "client");
            Thread server = new Thread(
                    () -> {
                        try {
                            Handle handle = new Handle(serverSck.getOutputStream(), serverSck.getInputStream());
                            Request request = handle.readRequest();
                            System.out.println(request);
                            handle.writeResponse(Response.okMessage(request, "hi!"));
                        } catch (IOException e) {
                            fail(e);
                        }
                    },
                    "server");

            server.start();
            client.start();

            server.join();
            client.join();
        }
    }
}
