import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class DmdbCardViewHandler implements HttpHandler {
	
	final int[] cardVideoIds;
	String baseQuery = "SELECT card_name, cost, civilization, card_type, race, power, card_text FROM Card JOIN RulesText USING (card_id) WHERE card_id=";
	
	public DmdbCardViewHandler() {
		List<String> videoList = new ArrayList<String>();
		try {
			videoList = Files.readAllLines(Paths.get("resources/dmdb-video-list.txt"));
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		cardVideoIds = new int[videoList.size()];
		for(int i = 0; i<cardVideoIds.length; i++) {
			String line = videoList.get(i);
			int card_id = Integer.parseInt(line.split(":")[0]);
			cardVideoIds[i] = card_id;
		}
		Arrays.sort(cardVideoIds);
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		try {
			int card_id = getCardId(exchange.getRequestURI().getPath());
			boolean hasVideo = checkForVideo(card_id);
			String htmlString = buildHTMLView(card_id, hasVideo);
			byte[] htmlBytes = htmlString.getBytes("UTF-8");
			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
			exchange.sendResponseHeaders(200, htmlBytes.length);
			try(OutputStream stream = exchange.getResponseBody()) {
				stream.write(htmlBytes);
			} finally {
				exchange.close();
			}
		} catch(NumberFormatException nfe) {
			sendErrorResponse(exchange, 400, "Invalid card ID.");
		} catch(SQLException sqle) {
			sqle.printStackTrace();
			sendErrorResponse(exchange, 500, "Database error.");
		} catch (Exception e) {
			e.printStackTrace();
			sendErrorResponse(exchange, 500, "An unexpected error occurred.");
		}
	}
	
	public int getCardId(String path) throws NumberFormatException {
		String[] paths = path.split("/");
		int id = Integer.parseInt(paths[paths.length-1]);
		return id;
	}
	
	public boolean checkForVideo(int card_id) {
		int a = Arrays.binarySearch(cardVideoIds, card_id);
		if(a>=0) return true;
		return false;
	}
	
	public String buildHTMLView(int card_id, boolean hasVideo) throws SQLException {
		StringBuilder cardViewBuilder = new StringBuilder();
		String sqlQuery = baseQuery + card_id;
		Connection conn = DatabaseConnectionManager.getConnection();
		try(Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sqlQuery)) {
			String idString = Integer.toString(card_id);
			if(idString.length()<4) {
				StringBuilder idBuilder = new StringBuilder();
				for(int i = 0; i<4-idString.length(); i++) {
					idBuilder.append('0');
				}
				idBuilder.append(idString);
				idString = idBuilder.toString();
			}
			String card_name = rs.getString("card_name");
			int cost = rs.getInt("cost");
			String[] civs = rs.getString("civilization").split("/");
			String card_type = rs.getString("card_type");
			String race = rs.getString("race");
			String power = rs.getString("power");
			String card_text = rs.getString("card_text");
			cardViewBuilder.append("<!DOCTYPE html>\n<html>\n<head>\n\t<meta charset=\"utf-8\">\n\t")
						   .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
						   .append("\t<title>").append(card_name).append("</title>\n\t")
						   .append("<link rel=\"stylesheet\" href=\"/dmdb-cardview-styles.css\">\n\t")
						   .append("<script src=\"/dmdb-cardview-scripts.js\" defer></script>\n</head>\n")
						   .append("<body style=\"background: ");
			String[] civColors = new String[civs.length];
			for(int i = 0; i<civs.length; i++) {
				String civ = civs[i];
				String color = "#808080"; //default to gray
				switch(civ) {
					case "light":
						color = "#ffd700";
						break;
					case "water":
						color = "#0ff";
						break;
					case "darkness":
						color = "#300030";
						break;
					case "fire":
						color = "#900";
						break;
					case "nature":
						color = "#228b22";
				}
				civColors[i] = color;
			}
			if(civColors.length==1) {
				cardViewBuilder.append(civColors[0]).append(";\">\n");
			} else if(civColors.length==2) {
				cardViewBuilder.append("linear-gradient(to right,")
							   .append(civColors[0]).append(" 0%,")
							   .append(civColors[0]).append(" 35%,")
							   .append(civColors[1]).append(" 65%,")
							   .append(civColors[1]).append(" 100%);\">\n");
			} else if(civColors.length==3) {
				cardViewBuilder.append("linear-gradient(to right,")
							   .append(civColors[0]).append(" 0%,")
							   .append(civColors[0]).append(" 20%,")
							   .append(civColors[1]).append(" 40%,")
							   .append(civColors[1]).append(" 60%,")
							   .append(civColors[2]).append(" 80%,")
							   .append(civColors[2]).append(" 100%);\">\n");
			} else {
				cardViewBuilder.append("linear-gradient(to right,");
				for(int i = 0; i<civColors.length; i++) {
					cardViewBuilder.append(civColors[i]);
					if(i<civColors.length-1) cardViewBuilder.append(",");
				}
				cardViewBuilder.append(");\">\n");
			}
			cardViewBuilder.append("\t<div id=\"profile-container\">\n\t\t<div id=\"media-container\">\n");
			if(hasVideo) {
				cardViewBuilder.append("\t\t\t<div id=\"video-container\">\n")
							   .append("\t\t\t\t<video src=\"https://media.duelmasters.us/")
							   .append(idString).append(".mp4\" autoplay muted></video>\n\t\t\t</div>\n");
			}
			cardViewBuilder.append("\t\t\t<img src=\"https://img.duelmasters.us/").append(idString).append(".webp\"");
			if(hasVideo) {
				cardViewBuilder.append(" style=\"display: none;\">\n");
			} else cardViewBuilder.append(">\n");
			cardViewBuilder.append("\t\t</div>\n\t\t<div id=\"card-info-container\">\n")
						   .append("\t\t\t<p><b>Name:</b> <a href=\"https://duelmasters.fandom.com/wiki/")
						   .append(card_name).append("\"").append(card_name).append("</a></p>\n")
						   .append("\t\t\t<p><b>Civilization:</b> ");
			for(int i = 0; i<civs.length; i++) {
				String civ = civs[i];
				cardViewBuilder.append(civ.substring(0,1).toUpperCase()).append(civ.substring(1))
							   .append(" <img src=\"/icons/civs/").append(civ).append(".webp\">");
				if(i<civs.length-1) cardViewBuilder.append(" / ");
			}
			cardViewBuilder.append("</p>\n")
						   .append("\t\t\t<p><b>Card Type:</b> ").append(card_type).append("</p>\n")
						   .append("\t\t\t<p><b>Cost:</b> ").append(cost).append("</p>\n");
			if(race!=null) {
				StringBuilder raceBuilder = new StringBuilder();
				String[] rarr = race.split("/");
				for(int i = 0; i<rarr.length; i++) {
					String str = rarr[i];
					if("fishy".equals(str)) raceBuilder.append("Fish");
					else if("gianto".equals(str)) raceBuilder.append("Giant");
					else {
						raceBuilder.append(str.substring(0,1).toUpperCase());
						for(int j = 1; j<str.length(); j++) {
							if(str.charAt(j)=='_') raceBuilder.append(' ');
							else if(str.charAt(j-1)=='_') raceBuilder.append(str.substring(j,j+1).toUpperCase());
							else raceBuilder.append(str.charAt(j));
						}
					}
					if(i<rarr.length-1) raceBuilder.append(" / ");
				}
				race = raceBuilder.toString();
				cardViewBuilder.append("\t\t\t<p><b>Race:</b> ").append(race).append("</p>\n");
			}
			if(power!=null) {
				cardViewBuilder.append("\t\t\t<p><b>Power:</b> ").append(power).append("</p>\n");
			}
			if(card_text.length()>0) {
				//System.out.println("Card text for " + card_name + ":");
				String[] textArr = card_text.split("\u25A0");
				cardViewBuilder.append("\t\t\t<p><b>Card Rules Text:</b></p>\n")
							   .append("\t\t\t<ul id=\"rules-text\">\n");
				for(String textItem : textArr) {
					if(textItem.length()>4) {
						//System.out.println("Text Item: " + textItem + " (Length = " + textItem.length() + ")");
						textItem = textItem.replace("(", "<i>(");
						textItem = textItem.replace(")", ")</i>");
						cardViewBuilder.append("\t\t\t\t<li>").append(textItem).append("</li>\n");
					}
				}
				cardViewBuilder.append("\t\t\t</ul>\n");
			}
			if(hasVideo) cardViewBuilder.append("\t\t\t<button id=\"toggle-btn\">Stop video</button>\n");
			cardViewBuilder.append("\t\t</div>\n\t</div>\n</body>\n</html>");	
		} 
		return cardViewBuilder.toString();
	}
	
	void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
		exchange.sendResponseHeaders(statusCode, errorMessage.length());
		try (OutputStream stream = exchange.getResponseBody()) {
			stream.write(errorMessage.getBytes());
		} finally {
			exchange.close();
		}
	}
}