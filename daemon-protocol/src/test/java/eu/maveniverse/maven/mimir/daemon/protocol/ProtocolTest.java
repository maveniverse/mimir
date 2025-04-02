package eu.maveniverse.maven.mimir.daemon.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

public class ProtocolTest {
    @Test
    void smoke() throws Exception {
        CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();
        try (ServerSocketChannel serverSocket =
                        ServerSocketChannel.open(StandardProtocolFamily.INET).bind(new InetSocketAddress(0));
                SocketChannel clientSck = SocketChannel.open(serverSocket.getLocalAddress());
                SocketChannel serverSck = serverSocket.accept()) {
            Thread client = new Thread(
                    () -> {
                        try {
                            Handle handle = new Handle(clientSck);
                            handle.writeRequest(Request.hello(Map.of("hello", "world")));
                            messages.add(handle.readResponse());
                        } catch (IOException e) {
                            fail(e);
                        }
                    },
                    "client");
            Thread server = new Thread(
                    () -> {
                        try {
                            Handle handle = new Handle(serverSck);
                            Request request = handle.readRequest();
                            messages.add(request);
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

        assertEquals(2, messages.size());
        Message first = messages.get(0);
        Message second = messages.get(1);

        assertInstanceOf(Request.class, first, first.toString());
        assertInstanceOf(Response.class, second, second.toString());

        assertEquals(Request.CMD_HELLO, ((Request) first).cmd());
        assertEquals("world", first.requireData("hello"));
        assertEquals(Response.STATUS_OK, ((Response) second).status());
        assertEquals("hi!", second.requireData(Response.DATA_MESSAGE));
    }
}
