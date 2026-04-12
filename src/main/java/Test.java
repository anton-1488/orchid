import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

public class Test {
    public static void main(String[] args) throws Exception {

    }

    private static void checkTor() throws Exception {
        TorClient client = new TorClient();
        client.addInitializationListener(new TorInitializationListener() {
            public void initializationProgress(String message, int percent) {
                System.out.println("Загрузка: " + percent + "% - " + message);
            }
            public void initializationCompleted() {
                System.out.println("Tor готов!");
            }
        });

        client.start();
        client.waitUntilReady();

        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 9050));

        Socket socket = new Socket(proxy);
        socket.connect(InetSocketAddress.createUnresolved("check.torproject.org", 80));
        System.out.println("Победа! Мы в сети Tor.");
        socket.close();
        client.stop();
    }
}