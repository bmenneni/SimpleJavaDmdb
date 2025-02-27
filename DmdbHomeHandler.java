import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.nio.file.Files;
import java.net.URI;

public class DmdbHomeHandler implements HttpHandler {
	
	protected String baseHtml;
	
	public DmdbHomeHandler() {
		try {
			this.baseHtml = loadHtmlFile("resources/dmdb.html");
		}
		catch (IOException ioe) {
			this.baseHtml = "<html><body><p>Error loading HTML file.</p></body></html>";
			ioe.printStackTrace();
		}
	}
	
	public void handle(HttpExchange exchange) throws IOException {	

		String requestPath = exchange.getRequestURI().getPath();
		String requestMethod = exchange.getRequestMethod();
		
		if("/".equals(requestPath)) {
			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
			if("GET".equals(requestMethod)) {
				exchange.sendResponseHeaders(200, baseHtml.length());
				try(OutputStream stream = exchange.getResponseBody()) {
					stream.write(baseHtml.getBytes());
				}			
			}
			else if("HEAD".equals(requestMethod)) {
				exchange.sendResponseHeaders(200, -1);
			}
		}
		else if("HEAD".equals(requestMethod)) exchange.sendResponseHeaders(404, -1);
		else {
			File fil = new File("resources" + requestPath);
			if(fil.exists()) {
				String fileContentType = getContentType(fil);
				exchange.getResponseHeaders().set("Content-Type", fileContentType);
				byte[] fileBytes = Files.readAllBytes(fil.toPath());
				exchange.sendResponseHeaders(200, fileBytes.length);
				try(OutputStream stream = exchange.getResponseBody()) {
					stream.write(fileBytes);
				}
			}
			else {
				String errorMsg = "404 Not Found";
				exchange.sendResponseHeaders(404, errorMsg.length());
				try(OutputStream stream = exchange.getResponseBody()) {
					stream.write(errorMsg.getBytes());
				}
			}
		}
	}
	
	public String loadHtmlFile(String filePath) throws IOException {
		StringBuilder bobTheHtmlBuilder = new StringBuilder();
		try(BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while((line = reader.readLine()) != null) {
				bobTheHtmlBuilder.append(line).append("\n");
			}
		}
		return bobTheHtmlBuilder.toString();
	}
	
	public String getContentType(File file) {
		String fileName = file.getName().toLowerCase();
		if(fileName.endsWith(".html")) {
			return "text/html; charset=UTF-8";
		}
		else if(fileName.endsWith(".jpg")) {
			return "image/jpeg";
		}
		else if(fileName.endsWith(".png")) {
			return "image/png";
		}
		else if(fileName.endsWith(".webp")) {
			return "image/webp";
		}
		else if(fileName.endsWith(".css")) {
			return "text/css";
		}
		else if(fileName.endsWith(".js")) {
			return "application/javascript";
		}
		else return "application/octet-stream";
	}
}