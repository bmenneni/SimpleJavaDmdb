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
			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
			exchange.sendResponseHeaders(200, htmlResponse.getBytes("UTF-8").length);
			try (OutputStream stream = exchange.getResponseBody()) {
				stream.write(htmlResponse.getBytes("UTF-8"));
			} finally {
				exchange.close();
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
		if(query==null||query.length()==0) return paramsMap;
		for(String param : query.split("&")) {
			String[] pair = param.split("=");
			if(pair.length>1) {
				paramsMap.computeIfAbsent(pair[0], k -> new ArrayList<>()).add(pair[1]);
			}
		}
		return paramsMap;
	}
	
	public ResultSet queryDmdb(HashMap<String, List<String>> paramsMap) throws SQLException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		sqlQueryBuilder.append("SELECT * FROM Card");
		boolean initialParam = true;
		String compareOperator = "";
		StringBuilder sorter = new StringBuilder();
		String matchMode = "";
		String sortMode = "";
		if(paramsMap.containsKey("match_civ")) {
			matchMode = paramsMap.get("match_civ").get(0);
		}
		Set<Map.Entry<String, List<String>>> paramsSet = paramsMap.entrySet();
		for(Map.Entry<String, List<String>> entry : paramsSet) {
			String sKey = entry.getKey();
			List<String> sVals = entry.getValue();
			if(initialParam&&!paramsSet.isEmpty()&&!"compare".equals(sKey)&&!"match_civ".equals(sKey)&&!"sort_by".equals(sKey)&&!"mode".equals(sKey)) {
				sqlQueryBuilder.append(" WHERE ");
				initialParam = false;
			}
			else if(!initialParam&&!"compare".equals(sKey)&&!"match_civ".equals(sKey)&&!"sort_by".equals(sKey)&&!"mode".equals(sKey)) {
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
				boolean tcgOnly = false;
				String tcgOnlyQuery = "(card_id<901 OR (card_id>9000 AND card_id<9081))";
				if(sVals.size()==1&&sVals.contains("tcg_only")) {
					sqlQueryBuilder.append(tcgOnlyQuery);
				}
				else if(sVals.size()>1) {
					String[] setVals = new String[sVals.size()];
					setVals = sVals.toArray(setVals);
					sqlQueryBuilder.append("(");
					boolean firstSet = true;
					for(int i = 0; i<setVals.length; i++) {
						if("tcg_only".equals(setVals[i])) {
							tcgOnly = true;
							continue;
						}
						if(!firstSet) {
							sqlQueryBuilder.append(" OR ");
						}
						sqlQueryBuilder.append(sKey + "='" + setVals[i] + "'");
						if(firstSet) firstSet = false;
					}
					sqlQueryBuilder.append(")");
					if(tcgOnly) sqlQueryBuilder.append(" AND " + tcgOnlyQuery);
				}
				else {
					sqlQueryBuilder.append(sKey + "='" + sVals.get(0) + "'");
				}
			}
			else if("civilization".equals(sKey)) {
				if("exact".equals(matchMode)) {
					if(sVals.size()==1) {
						String civname = sVals.get(0);
						civname = civname.substring(0,1).toUpperCase() + civname.substring(1);
						sqlQueryBuilder.append(sKey + "='" + civname + "'");
					} else {
						String[] civArr = new String[sVals.size()];
						civArr = sVals.toArray(civArr);
						sqlQueryBuilder.append("(");
						for(int i = 0; i<civArr.length; i++) {
							sqlQueryBuilder.append(sKey + " LIKE \"%" + civArr[i] + "%\" AND ");
						}
						int delimiterCount = civArr.length-1;
						sqlQueryBuilder.append("(LENGTH(civilization) - LENGTH(REPLACE(civilization, '/', ''))) = " + delimiterCount);
						sqlQueryBuilder.append(")");
					}
				} else {
					String[] arr = new String[sVals.size()];
					arr = sVals.toArray(arr);
					for(int i = 0; i<arr.length; i++) {
						if(i>0) sqlQueryBuilder.append(" OR ");
						sqlQueryBuilder.append(sKey + " LIKE \"%" + arr[i] + "%\"");
					}
				}
			}
			else if("keywords".equals(sKey)||"categories".equals(sKey)) {
				String[] arr = new String[sVals.size()];
				arr = sVals.toArray(arr);
				for(int i = 0; i<arr.length; i++) {
					if(i>0) sqlQueryBuilder.append(" AND ");
					sqlQueryBuilder.append(sKey + " LIKE \"%" + arr[i] + "%\"");
				}
			}
			else if("race".equals(sKey)||"card_type".equals(sKey)) {
				sqlQueryBuilder.append(sKey + " LIKE \"%" + sVals.get(0) + "%\""); 
			}
			else if("sort_by".equals(sKey)) {
				if("rarity".equals(sVals.get(0))) {
					sorter.append(" ORDER BY CASE WHEN rarity='NR' THEN 0")
						  .append(" WHEN rarity='C' THEN 1")
						  .append(" WHEN rarity='U' THEN 2")
						  .append(" WHEN rarity='R' THEN 3")
						  .append(" WHEN rarity='VR' THEN 4")
						  .append(" WHEN rarity='SR' THEN 5")
						  .append(" end;");
				}
				else sorter.append(" ORDER BY " + sVals.get(0));
			}
			else if("mode".equals(sKey)) {
				sortMode = sVals.get(0);
			}
		}
		String sortString = sorter.toString();
		sqlQueryBuilder.append(sortString);
		sqlQueryBuilder.append(" " + sortMode);
		if(sortString.length()>0) sqlQueryBuilder.append(" NULLS LAST");
		String sqlQuery = sqlQueryBuilder.toString();
		System.out.println(sqlQuery);
		Statement statement = conn.createStatement();
		ResultSet rs = statement.executeQuery(sqlQuery);
		return rs;
	}
	
	public String buildResultsTable(ResultSet rs) throws SQLException {
		StringBuilder resultsTableBuilder = new StringBuilder();
		resultsTableBuilder.append("<p class=\"small-headline\">TIP: Shift-click on a search result to open the card image in a new window.</p>\n")
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
			resultsTableBuilder.append("\t<tr class=\"results-row\" ");
			String card_id = rs.getString("card_id");
			if(card_id.length()<4) {
				StringBuilder idBuilder = new StringBuilder();
				for(int i = 0; i<4-card_id.length(); i++) {
					idBuilder.append('0');
				}
				idBuilder.append(card_id);
				card_id = idBuilder.toString();
			}
			resultsTableBuilder.append("data-id=\"" + card_id + "\">\n")
							   .append("\t\t<td>" + rowCount + ". </td>\n");
			String cardname = rs.getString("card_name");
			if(cardname.length()>10&&"Uberdragon".equals(cardname.substring(0,10))) {
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
			resultsTableBuilder.append("\t\t<td>" + card_power + "</td>\n");
			if(rs.getString("rarity").equals("NR")) {
				resultsTableBuilder.append("\t\t<td class=\"center-td\" title=\"No Rarity\">NR</td>\n");
			} else {
				String rarity = rs.getString("rarity").toLowerCase();
				String rarityFullName = "";
				switch(rarity) {
					case "c":
						rarityFullName = "Common";
						break;
					case "u":
						rarityFullName = "Uncommon";
						break;
					case "r":
						rarityFullName = "Rare";
						break;
					case "vr":
						rarityFullName = "Very Rare";
						break;
					case "sr":
						rarityFullName = "Super Rare";
				}
				resultsTableBuilder.append("\t\t<td class=\"center-td\"><img src=\"icons/rarity-" + rarity + ".png\" title=\"" + rarityFullName + "\"></td>\n");
			}
			resultsTableBuilder.append("\t\t<td>" + rs.getString("coll_num") + "</td>\n");
			String card_set = rs.getString("card_set");
			resultsTableBuilder.append("\t\t<td class=\"center-td\"><img src=\"icons/" + card_set + ".png\" title=\"" + card_set.toUpperCase() + "\"></td>\n")
							   .append("\t</tr>\n");
		}
		resultsTableBuilder.append("</tbody>\n</table>\n");
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
				else if(line.contains("match_filter")&&params.containsKey("match_civ")) {
					String matchMode = params.get("match_civ").get(0);
					String[] arr = line.split("\"");
					String val = arr[arr.length-2];
					if(line.contains("checked")&&!(val.equals(matchMode))) {
						String[] nonchecked = line.split("checked");
						htmlResponseBuilder.append(nonchecked[0] + nonchecked[1] + "\n");
					}
					else if(!(line.contains("checked"))&&val.equals(matchMode)) {
						String[] checked = line.split(">");
						htmlResponseBuilder.append(checked[0] + " checked>" + "\n");
					}
					else htmlResponseBuilder.append(line + "\n");
				}
				else if(line.contains("/form")) {
					htmlResponseBuilder.append(line).append("\n").append(resultsTable);
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
		} finally {
			exchange.close();
		}
	}
}