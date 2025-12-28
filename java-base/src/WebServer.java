import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import com.sun.net.httpserver.*;


public class WebServer {
    public static void main(String[] args) throws IOException {
        // check what listening port is requested
        if (args.length != 1)
            throw new IllegalArgumentException("Port number not detected. " +
                            "You must pass a command line" +
					       " argument representing the port that this servers should be" +
					       " bound to when running this program.");

        // save the port
        int portNumber;
        try {portNumber = Integer.parseInt(args[0].trim());}
        catch (Exception e) {throw new IllegalArgumentException(Integer.parseInt(args[0].trim()) + " is " +
                            " an invalid port number. You must pass a command line" +
					       " argument representing the port that this servers should be" +
					       " bound to when running this program.  Or a Query string.");}
        

        // start the server
        InetSocketAddress address = new InetSocketAddress(portNumber);
        HttpServer server = HttpServer.create(address,8);
        HttpContext context = server.createContext("/");
        context.setHandler(WebServer::requestHandler);
        server.start();
        System.out.println("Web Server Started.");
    }

    public static void requestHandler(HttpExchange exchange) {
        // get path always has a leading slash "/"
        String requestFilePath = exchange.getRequestURI().getPath();
        System.out.println("Request incoming for:  " + requestFilePath);
        File inFile = new File(requestFilePath.substring(1));
        
        /**
         * ROUTER
        */
        // TODO: direct example from class, should be modified
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

        // route for asset requests that exist
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

        // route for anything else (404?)
        else 
        {
            String response = "<p><b>404 NOT FOUND</p>";

            exchange.getResponseHeaders().add("Content-type", "text/html");
            exchange.sendResponseHeaders(404, response.length());

            OutputStream responseStream = exchange.getResponseBody();
            responseStream.write(response.getBytes());
        }
    }

    // Since we cannot run a public webserver on the department's linux
    // machines, we are using a cgi script to pass the query argument to
    // the method below, and then displaying a response to standard out.
    // TODO: Unsure how this works. Look into it
    public static void handleSingleResponse(String query) {
        try {
            query = URLDecoder.decode(query, StandardCharsets.UTF_8);
            Map<String,String> keyValuePairs = parseQuery(query);
            
            // create backend and frontend objects to respond to this request
            FrontendInterface frontend = createWorkingFrontend("./campus.dot");
            // compute answer to user's requested problem based on query args:
            String response = generateResponseHTML(keyValuePairs,frontend);
            // generate HTML prompts for user for make next requests
            String prompts = generatePromptHTML(frontend);
            // compose response and prompts into a complete html template
            String html = composeHTML(response,prompts);
            
            System.out.println(html);
                            
            // unless something goes wrong, in which case report problem
        } catch (Exception e) {
            System.out.println("Exception Thrown: "+e.toString());
            e.printStackTrace();
        }
    }





    /**
     * LEFTOVER CODE FROM INTEGRATION WEB APPLICATION
     */

    // reads key value pairs from the query string of a URI into a map
    private static Map<String,String> parseQuery(String query) {
	HashMap<String,String> map = new HashMap<>();
	if(query != null && query.contains("="))
	    Stream.of(query.split("&")).forEach(arg -> {
		    String[] pair = arg.split("=");
		    if(pair.length != 2)
			throw new IllegalArgumentException("Unable to split "+
							   "arg: " + arg+" into a key value pair around a "+
							   "single = delimiter.");
		    map.put(pair[0],pair[1]);
		});
	return map;
    }

    // creates a working Frontend, Backend, DijkstraGraph, and HashtableMap
    private static FrontendInterface createWorkingFrontend(String filename) throws IOException {
	GraphADT<String,Double> graph = new DijkstraGraph<>();
	BackendInterface backend = new Backend(graph);
	backend.loadGraphData(filename);			
	FrontendInterface frontend = new Frontend(backend);
	return frontend;
    }

    // creates the html response for the kind of question requested (if any)
    private static String generateResponseHTML(Map<String,String> keyValuePairs, FrontendInterface frontend) {
	// compute response for shortest path request
	String response = "<div id=\"response\">";
	if(keyValuePairs.containsKey("start") &&
	   keyValuePairs.containsKey("end")) {
	    response += frontend.generateShortestPathResponseHTML(
								  keyValuePairs.get("start"),
								  keyValuePairs.get("end")) + "</div>";
	    // compute response for other request
	} else if(keyValuePairs.containsKey("from")) {
	    response += frontend.generateLongestLocationListFromResponseHTML(												keyValuePairs.get("from")) + "</div>";
	    // otherwise, leave response div blank 
	} else
	    response += "</div>";

	return response;
    }

    // generate separate div sections with a prompt for each kind of request
    private static String generatePromptHTML(FrontendInterface frontend) {
	String firstPrompt = "<div id=\"firstPrompt\">" +
	    frontend.generateShortestPathPromptHTML() + "</div>";
	String secondPrompt = "<div id=\"secondPrompt\">" +
	    frontend.generateLongestLocationListFromPromptHTML() + "</div>";
	return firstPrompt + secondPrompt;
    }

    // compose reponse with prompts inside a complete html tree
    private static String composeHTML(String response, String prompts) throws IOException {
	// read contents of template file into html string
	String html = "";
	Scanner in = new Scanner(new File("template.html"));
	while(in.hasNextLine()) html += in.nextLine() + "\n";

	// replace placeholders for response and prompts
	html = html.replaceFirst("<!-- RESPONSE GOES HERE -->",response);
	html = html.replaceFirst("<!-- PROMPTS GO HERE -->",prompts);
	
	return html;
    }

}