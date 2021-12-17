import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CloudServer {
        public void startServer(){
            ServerSocket srv = null;
            try {
                srv = new ServerSocket(8081);

                System.out.println("Server started...");
                while (true) {
                    Socket socket = srv.accept();
                    Handler handler = new Handler(socket);
                    new Thread(handler).start();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }

    }

}


