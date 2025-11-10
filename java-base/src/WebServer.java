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

                // route /graphSearch expects start= and end= variables
                if (requestFilePath.equals("/graphSearch"))
                {
                    String queryString = exchange.getRequestURI().getQuery();
                    String startLoc = null, endLoc = null;
                    for (String element : queryString.split("&")) {
                        if (element.startsWith("start=")) 
                            startLoc = element.substring(6);

                        if (element.startsWith("end="))
                            endLoc = element.substring(4);
                    }
                    System.out.println("startLoc: " + startLoc + " --- endLoc: " + endLoc);

                    String placeholderResults = "<h3>Shortest Path (placeholder server)</h3>" 
                        + "<ul>"
                        + "\t<li>" + startLoc + "</li>"
                        + "\t<li>" + "placeholder webserver" + "</li>"
                        + "\t<li>" + endLoc + "</li>"
                        + "</ul>";

                    exchange.getResponseHeaders().add("Content-type", "text/html");
                    // length is the number of bytes (or chars) in the string we are about to send
                    exchange.sendResponseHeaders(200, placeholderResults.length()); 

                    OutputStream responseStream = exchange.getResponseBody();
                    responseStream.write(placeholderResults.getBytes());
                    responseStream.close();
                }
                // "route" for assets
                else if (inFile.exists())
                {
                    String contextType = "text/html";
                    if (requestFilePath.endsWith(".css")) contextType = "text/css";
                    if (requestFilePath.endsWith(".png")) contextType = "image/png";

                    exchange.getResponseHeaders().add("Content-type" , contextType);
                    exchange.sendResponseHeaders(200, inFile.length());

                    Files.copy(inFile.toPath(), exchange.getResponseBody());
                    exchange.getResponseBody().close();
                }
                // default route
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