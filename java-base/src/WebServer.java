package calculator.matrix;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

import com.sun.net.httpserver.*;
import com.sun.net.httpserver.HttpHandler;


//This is a demonstration of a basic java web server done in class CS400
public class WebServer {


    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        HttpContext context = server.createContext("/");
        context.setHandler(new HttpHandler() {
           
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                // get path always has a leading slash "/"
                String requestFilePath = exchange.getRequestURI().getPath();
                System.out.println("Requeest incoming for:  " + requestFilePath);

                File inFile = new File(requestFilePath.substring(1));

                if (inFile.exists())
                {
                    String contextType = "text/html";
                    if (requestFilePath.endsWith(".css")) contentType = "text/css";
                    if (requestFilePath.endsWith(".png")) contentType = "image/png";

                    exchange.getResponseHeaders().add("Content-type" , contextType);
                    exchange.sendResponseHeaders(200, inFile.length());

                    Files.copy(inFile.toPath(), exchange.getResponseBody());
                    exchange.getResponseBody().close();
                }

                else 
                {
                    String response = "<p>Hello Web Server...</p>";

                    exchange.getResponseHeaders().add("Content-type", "text/html");
                    exchange.sendResponseHeaders(200, response.length());

                    OutputStream responseStream = exchange.getResponseBody();
                    responseStream.write(response.getBytes());
                }
            }
        });

        server.start();
        System.out.println("Server started...");
    }

}