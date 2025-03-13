import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Main {
	public static void main(String[] args) throws Exception {
		InetAddress myAddress = InetAddress.getByName("0.0.0.0");
		HttpServer myServer = HttpServer.create(new InetSocketAddress(myAddress, 8080), 0);
		HttpContext homeContext = myServer.createContext("/", new DmdbHomeHandler());
		HttpContext searchContext = myServer.createContext("/search", new DmdbSearchHandler());
		myServer.start();
		System.out.println("Server started on port 8080.");
	}
}