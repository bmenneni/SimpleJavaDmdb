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
import java.util.*;

public class DmdbSearchHandler implements HttpHandler {
	
	private Connection conn;	
	
	public DmdbSearchHandler() throws SQLException {
		conn = DriverManager.getConnection("jdbc:sqlite:duelmasters.db");
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		try {
			HashMap<String, List<String>> params = parseQueryParams(exchange.getRequestURI().getQuery());
			ResultSet rs = queryDmdb(params);
			String resultsTable = buildResultsTable(rs);
			String htmlResponse = buildHtmlResponse("resources/dmdb.html", params, resultsTable);
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
	
	public HashMap<String, List<String>> parseQueryParams(String query) {
		HashMap<String, List<String>> paramsMap = new HashMap<>();
		String[] queryComponents = query.split("&");
		for(int i = 0; i<queryComponents.length; i++) {
			String paramKey = queryComponents[i].split("=")[0];
			if(paramsMap.containsKey(paramKey)) continue;
			else {
				ArrayList<String> paramVals = new ArrayList<>();
				for(String component : queryComponents) {
					String[] queryElements = component.split("=");
					if(queryElements.length>1&&queryElements[0].equals(paramKey)) {
						paramVals.add(queryElements[1]);
					}
				}
				if(paramKey.length()>0&&!paramVals.isEmpty()) {
					paramsMap.put(paramKey, paramVals);					
				}
			}
		}
		return paramsMap;
	}
	
	public ResultSet queryDmdb(HashMap<String, List<String>> paramsMap) throws SQLException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM Card");
		boolean initialParam = true;
		String compareOperator = "";
		Set<Map.Entry<String, List<String>>> paramsSet = paramsMap.entrySet();
		for(Map.Entry<String, List<String>> entry : paramsSet) {
			String sKey = entry.getKey();
			List<String> sVals = entry.getValue();
			if(initialParam&&!paramsSet.isEmpty()&&!"compare".equals(sKey)) {
				sqlQueryBuilder.append(" WHERE ");
				initialParam = false;
			}
			else if(!initialParam&&!"compare".equals(sKey)) {
				sqlQueryBuilder.append(" AND ");
			}
			if("compare".equals(sKey)&&compareOperator.equals("")) {
				String compareVal = sVals.get(0);
				switch (compareVal) {
					case "greater_or_equal":
						compareOperator = ">=";
						break;
					case "less_or_equal":
						compareOperator = "<=";
				}
			}
			else if("cost".equals(sKey)) {
				if(">=".equals(compareOperator)||"<=".equals(compareOperator)) {
					sqlQueryBuilder.append(sKey + compareOperator + sVals.get(0));
				}
				else sqlQueryBuilder.append(sKey + "=" + sVals.get(0));
			}
			else if("rarity".equals(sKey)) {
				sqlQueryBuilder.append(sKey + "= '" + sVals.get(0).toUpperCase() + "'");
			}
			else if("card_name".equals(sKey)) {
				char[] chars = sVals.get(0).toCharArray();
				for(int i = 0; i<chars.length; i++) {
					switch(chars[i]) {
						case '+':
							chars[i]=' ';
							break;
						case '"':
							chars[i]='\'';
							break;
						case '\u00DC':
							chars[i]='U';
					}
				}
				String cardname = new String(chars);
				sqlQueryBuilder.append(sKey + " LIKE \"%" + cardname + "%\"");
			}
			else if("card_set".equals(sKey)) {
				if(sVals.size()>1) {
					String[] setVals = new String[sVals.size()];
					setVals = sVals.toArray(setVals);
					sqlQueryBuilder.append("(");
					for(int i = 0; i<setVals.length; i++) {
						if(i>0) {
							sqlQueryBuilder.append(" OR ");
						}
						sqlQueryBuilder.append(sKey + " = '" + setVals[i] + "'"); 
					}
					sqlQueryBuilder.append(")");
				}
				else {
					sqlQueryBuilder.append(sKey + "= '" + sVals.get(0) + "'");
				}
			}
			else if("civilization".equals(sKey)||"keywords".equals(sKey)||"categories".equals(sKey)) {
				String[] arr = new String[sVals.size()];
				arr = sVals.toArray(arr);
				for(int i = 0; i<arr.length; i++) {
					if(i>0) {
						sqlQueryBuilder.append(" AND ");
					}
					sqlQueryBuilder.append(sKey + " LIKE \"%" + arr[i] + "%\"");
				}
			}
			else if("race".equals(sKey)||"card_type".equals(sKey)) {
				sqlQueryBuilder.append(sKey + " LIKE \"%" + sVals.get(0) + "%\""); 
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
							   .append("\t\t<td>" + rowCount + ". </td>\n");
			String cardname = rs.getString("card_name");
			if(cardname.length()>4&&"Uber".equals(cardname.substring(0,4))) {
				cardname = "\u00DCber" + cardname.substring(4);
			}
			resultsTableBuilder.append("\t\t<td>" + cardname + "</td>\n")
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
	
	public String buildHtmlResponse(String baseFilePath, HashMap<String, List<String>> params, String resultsTable) throws IOException {
		StringBuilder htmlResponseBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(baseFilePath))) {
			String line;
			while((line = reader.readLine()) != null) {
				if(line.contains("option value=\"\" selected")) {
					if((line.contains("All Sets")&&params.containsKey("card_set"))
					    || (line.contains("All Civilizations")&&params.containsKey("civilization"))
						|| (line.contains("All Keywords")&&params.containsKey("keywords"))
						|| (line.contains("All Categories")&&params.containsKey("categories"))) {
							String[] arr = line.split("selected");
							htmlResponseBuilder.append(arr[0]+arr[1]+"\n");
						} else htmlResponseBuilder.append(line + "\n");
					}
				else if(line.contains("option value")) {
					String[] optionTag = line.split("\"");
					if(optionTag[1].length()>0&&params.values().stream().anyMatch(list -> list.contains(optionTag[1]))) {
						htmlResponseBuilder.append(optionTag[0] + "\"" + optionTag[1] + "\" selected" + optionTag[2]).append("\n");
					}
					else htmlResponseBuilder.append(line + "\n");
				}
				else if(line.contains("/form")) {
					htmlResponseBuilder.append(line).append("\n<br>\n").append(resultsTable);
				}
				else htmlResponseBuilder.append(line + "\n");
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