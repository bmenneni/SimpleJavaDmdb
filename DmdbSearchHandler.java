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
import java.util.HashMap;
import java.util.Map;

public class DmdbSearchHandler implements HttpHandler {
	
	private Connection conn;	
	private HashMap<String, String> optionMap;
	
	public DmdbSearchHandler() throws SQLException {
		conn = DriverManager.getConnection("jdbc:sqlite:duelmasters.db");
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		try {
			String[][] params = parseQueryParams(exchange.getRequestURI().getQuery());
			ResultSet rs = queryDmdb(params);
			String resultsTable = buildResultsTable(rs);
			String htmlResponse = buildHtmlResponse("resources/dmdb.html", resultsTable);
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
	
	public String[][] parseQueryParams(String query) {
		String[] queryComponents = query.split("&");
		String[][] queryElements = new String[queryComponents.length][];
		optionMap = new HashMap<>();
		for(int i = 0; i<queryComponents.length; i++) {
			queryElements[i] = queryComponents[i].split("=");
			if (queryElements[i].length>1) {
				String param = queryElements[i][0];
				String value = queryElements[i][1];
				optionMap.put(param, value);
			}
		}
		return queryElements;
	}
	
	public ResultSet queryDmdb(String[][] params) throws SQLException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM Card");
		int count = 0;
		String compareOperator = "";
		for(int i = 0; i<params.length; i++) {
			if(count>0 && params[i].length>1 && params[i][1]!=null && params[i][1].length()>0 && !"compare".equals(params[i][0])) {
				sqlQueryBuilder.append(" AND ");
				count++;
			}
			else if(count==0 && params[i].length>1 && params[i][1]!=null && params[i][1].length()>0 && !"compare".equals(params[i][0])) {
				sqlQueryBuilder.append(" WHERE ");
				count++;
			}
			String sKey = "";
			String sVal = "";
			if(params[i].length>1 && params[i][1]!=null) {
				sKey = params[i][0];
				sVal = params[i][1];
			}
			if("compare".equals(sKey)&&compareOperator.equals("")) {
				switch (sVal) {
				case "greater_or_equal": 
					compareOperator = ">=";
					break;
				case "less_or_equal":
					compareOperator = "<=";
					break;
				}
			}
			else if("cost".equals(sKey)) {
				if(">=".equals(compareOperator)||"<=".equals(compareOperator)) {
					sqlQueryBuilder.append(sKey + compareOperator + sVal);
				}
				else sqlQueryBuilder.append(sKey + "=" + sVal);
			}
			else if("rarity".equals(sKey)) {
				sqlQueryBuilder.append(sKey + " = '" + sVal.toUpperCase() + "'");
			}
			else if("card_set".equals(sKey)) {
				sqlQueryBuilder.append(sKey + " = '" + sVal + "'");
			}
			else if(sKey.length()>0) {
				sqlQueryBuilder.append(sKey + " LIKE \"%" + sVal + "%\"");
			}
		}
		String sqlQuery = sqlQueryBuilder.toString();
		System.out.println(sqlQuery);
		Statement statement = conn.createStatement();
		ResultSet rs = statement.executeQuery(sqlQuery);
		return rs;
	}
	
	public String buildResultsTable(ResultSet rs) throws SQLException {
		StringBuilder resultsTableBuilder = new StringBuilder();
		resultsTableBuilder.append("<div id=\"results-container\">\n")
						   .append("<table id=\"results-table\">\n")
						   .append("<thead>\n\t<tr>\n\t\t<th>\n\t\t\t<b>#</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Name</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Civilization</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Cost</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Type</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Race</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Power</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Rarity</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Col #</b>\n\t\t</th>\n")
						   .append("\t\t<th>\n\t\t\t<b>Set</b>\n\t\t</th>\n\t</tr>\n</thead>\n<tbody>\n");
		int rowCount = 0;
		while (rs.next()) {
			rowCount++;
			if(rowCount%2==1) {
			resultsTableBuilder.append("\t<tr style=\"background-color: rgb(255, 255, 255);\" ")
							   .append("onmouseout=\"this.style.backgroundColor='#FFFFFF'\" ");
			}
			else {
			resultsTableBuilder.append("\t<tr style=\"background-color: rgb(234, 234, 234);\" ")
							   .append("onmouseout=\"this.style.backgroundColor='#EAEAEA'\" ");
			}
			resultsTableBuilder.append("onmouseover=\"this.style.backgroundColor='#C5C5C5'\" ")
							   .append("onclick=\"openCard(this)\">\n")
							   .append("\t\t<td>" + rowCount + ". </td>\n")
							   .append("\t\t<td>" + rs.getString("card_name") + "</td>\n")
							   .append("\t\t<td>" + rs.getString("civilization") + "</td>\n")
							   .append("\t\t<td>" + rs.getInt("cost") + "</td>\n")
							   .append("\t\t<td>" + rs.getString("card_type") + "</td>\n");
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
			race = raceBuilder.toString();
			if(race.endsWith("Fishy")||race.endsWith("Gianto")) race = race.substring(0,race.length()-1);
			resultsTableBuilder.append("\t\t<td>" + race + "</td>\n");
			String card_power = rs.getString("power");
			if(card_power==null) {
				card_power = "";
			}
			resultsTableBuilder.append("\t\t<td>" + card_power + "</td>\n")
							   .append("\t\t<td>" + rs.getString("rarity") + "</td>\n")
							   .append("\t\t<td>" + rs.getString("coll_num") + "</td>\n");
			String card_set = rs.getString("card_set");
			resultsTableBuilder.append("\t\t<td class=\"set-td\"><img src=\"icons/" + card_set + ".png\" title=\"" + card_set.toUpperCase() + "\"></td>\n")
							   .append("\t</tr>\n");
		}
		resultsTableBuilder.append("</tbody>\n</table>\n</div>\n");
		return resultsTableBuilder.toString();
	}
	
	public String buildHtmlResponse(String baseFilePath, String resultsTable) throws IOException {
		StringBuilder htmlResponseBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(baseFilePath))) {
			String line;
			while((line = reader.readLine())!=null) {
				if(line.contains("option value")) {
					String[] optionTag = line.split("\"");
					if(optionTag[1].length()>0&&optionMap.containsValue(optionTag[1])&&optionTag.length==3) {
						htmlResponseBuilder.append(optionTag[0] + "\"" + optionTag[1] + "\" selected" + optionTag[2]).append("\n");
					}
					else htmlResponseBuilder.append(line).append("\n");
				}
				else if(line.contains("/form")) {
					htmlResponseBuilder.append(line).append("\n<br>\n").append(resultsTable);
				}
				else htmlResponseBuilder.append(line).append("\n");
			}
		}
		return htmlResponseBuilder.toString();
	}
	
	public void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
		exchange.sendResponseHeaders(statusCode, errorMessage.length());
		try (OutputStream stream = exchange.getResponseBody()) {
			stream.write(errorMessage.getBytes());
		}
	}
}