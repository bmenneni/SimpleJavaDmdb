import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.FileReader;

public class DmdbSearchHandler implements HttpHandler {
	
	private Connection conn;
	private String baseHtml;
	
	public DmdbSearchHandler() throws SQLException {
		this.conn = DriverManager.getConnection("jdbc:sqlite:duelmasters.db");
		try {
			this.baseHtml = loadFile("static/dmdb.html");
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		try {
			String[][] params = parseQueryParams(exchange.getRequestURI().getQuery());
			ResultSet rs = queryDmdb(params);
			String htmlResponse = buildResultsTable(rs);
			exchange.sendResponseHeaders(200, htmlResponse.getBytes().length);
			try (OutputStream stream = exchange.getResponseBody()) {
				stream.write(htmlResponse.getBytes());
			}
		}
		catch (SQLException sqle) {
			sqle.printStackTrace();
			sendErrorResponse(exchange, 500, "Database error.");
		}
		catch (Exception e) {
			e.printStackTrace();
			sendErrorResponse(exchange, 500, "An unexpected error occurred.");
		}
	}
	
	private String[][] parseQueryParams(String query) {
		String[] queryComponents = query.split("&");
		String[][] queryElements = new String[queryComponents.length][2];
		for(int i = 0; i<queryComponents.length; i++) {
			queryElements[i] = queryComponents[i].split("=");
		}
		return queryElements;
	}
	
	private ResultSet queryDmdb(String[][] params) throws SQLException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM Card");
		if(params[0].length>1&&params[0][1]!=null) sqlQueryBuilder.append(" WHERE ");
		for(int i = 0; i<params.length; i++) {
			String sKey = new String();
			String sVal = new String();
			if(params[i].length>1&&params[i][1]!=null) {
				sKey = params[i][0];
				sVal = params[i][1];
			}
			if("civilization".equals(sKey) || "race".equals(sKey) || "keywords".equals(sKey) || "categories".equals(sKey) || "card_name".equals(sKey)) {
					sqlQueryBuilder.append(sKey + " LIKE '%" + sVal +  "%'");
				}
			else if("rarity".equals(sKey) || "card_set".equals(sKey)) {
				sVal = sVal.toUpperCase();
				sqlQueryBuilder.append(sKey + " = '" + sVal + "'");
			}
			else if("card_type".equals(sKey)) {
				sVal = sVal.substring(0,1).toUpperCase() + sVal.substring(1);
				sqlQueryBuilder.append(sKey + " = '" + sVal + "'");
			}
			else if("cost".equals(sKey)) sqlQueryBuilder.append(sKey + " = '" + sVal + "'");
			if(params.length>0 && i!=(params.length-1)) {
				sqlQueryBuilder.append(" AND ");
			}
		}
		String sqlQuery = sqlQueryBuilder.toString();
		System.out.println(sqlQuery);
		Statement statement = conn.createStatement();
		ResultSet rs = statement.executeQuery(sqlQuery);
		return rs;
	}
	
	private String buildResultsTable(ResultSet rs) throws SQLException {
		StringBuilder resultsTableBuilder = new StringBuilder();
		String[] arr = baseHtml.split("</table>");
		String responseHead = arr[0] + "</table><br>";
		String responseFoot = arr[1];
		resultsTableBuilder.append(responseHead)
						   .append("<div style='float: left; margin-right: 20px;'>")
						   .append("<table id='resultsTable' cellpadding='3' cellspacing='1' width='750'")
						   .append("<thead><tr bgcolor='#C5C5C5'><th><b>#</b></th><th><b>Name</b></th><th><b>Civilization</b></th>")
						   .append("<th><b>Cost</b></th><th><b>Type</b></th><th><b>Race</b></th><th><b>Power</b></th><th><b>Rarity</b></th>")
						   .append("<th><b>Col #</b></th><th><b>Set</b></th></tr></thead><tbody>");
		int rowCount = 0;
		while (rs.next()) {
			rowCount++;
			resultsTableBuilder.append("<tr onclick='openCard(this, event)'>")
							   .append("<td>" + rowCount + ". </td>")
							   .append("<td>" + rs.getString("card_name") + "</td>")
							   .append("<td>" + rs.getString("civilization") + "</td>")
							   .append("<td>" + rs.getInt("cost") + "</td>")
							   .append("<td>" + rs.getString("card_type") + "</td>");
			String race = rs.getString("race");
			StringBuilder raceBuilder = new StringBuilder();
			if(race!=null) {
				raceBuilder.append(race.substring(0,1).toUpperCase());
				for(int i = 1; i<race.length(); i++) {
					if(race.charAt(i)=='_') {
						raceBuilder.append(' ');
					}
					else if(race.charAt(i-1)=='_' || race.charAt(i-1)=='/') {
						raceBuilder.append(race.substring(i,i+1).toUpperCase());
					}
					else {
						raceBuilder.append(race.charAt(i));
					}
				}
			}
			resultsTableBuilder.append("<td>" + raceBuilder.toString() + "</td>");
			String card_power = rs.getString("power");
			if(card_power==null) {
				card_power = "";
			}
			resultsTableBuilder.append("<td>" + card_power + "</td>")
							   .append("<td>" + rs.getString("rarity") + "</td>")
							   .append("<td>" + rs.getString("coll_num") + "</td>")
							   .append("<td>" + rs.getString("card_set") + "</td>")
							   .append("</tr>");
		}
		resultsTableBuilder.append("</tbody></table></div>")
						   .append(responseFoot);
		String resultsTable = resultsTableBuilder.toString();
		return resultsTable;
	}
	
	private String loadFile(String filePath) throws IOException {
		StringBuilder bobTheHtmlBuilder = new StringBuilder();
		try(BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while((line = reader.readLine()) != null) {
				bobTheHtmlBuilder.append(line).append("\n");
			}
		}
		return bobTheHtmlBuilder.toString();
	}
	
	private void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
		exchange.sendResponseHeaders(statusCode, errorMessage.length());
		try (OutputStream stream = exchange.getResponseBody()) {
			stream.write(errorMessage.getBytes());
		}
	}
}