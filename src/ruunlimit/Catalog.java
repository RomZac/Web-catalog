package ruunlimit;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet(urlPatterns = "/Catalog/*")
public class Catalog extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";
	private static final int CR = (int) '\r';
	private static final int LF = (int) '\n';
	public String web = "Offline";
	private int auth = -1;
	private ArrayList<Article> table_art = new ArrayList<>();
	private ConcurrentHashMap<String, String> table_log = new ConcurrentHashMap<>();

	public void init() {
		auth = -1;
		web = "Online";
		Scanner input;
		try {
			input = new Scanner(new FileReader("R:\\Server-file\\init.txt"));
			while (input.hasNextLine()) {
				String line = input.nextLine();
				String theme = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
				line = line.substring(line.indexOf(']') + 2);
				String author = line.substring(0, line.indexOf(' '));
				line = line.substring(line.indexOf(' ') + 1);
				int year = Integer.parseInt(line.substring(0, line.indexOf(' ')));
				line = line.substring(line.indexOf('['));
				ArrayList<String> keyword = new ArrayList<>();
				while (line.indexOf(']') > 1) {
					keyword.add(line.substring(0, line.indexOf(' ')));
					line = line.substring(line.indexOf(' ') + 1);
				}
				int number = Integer.parseInt(line.substring(0, line.indexOf(' '))); // the
																						// end
				line = line.substring(line.indexOf(' '));
				String user = line;
				Article cell = new Article(number, author, theme, year, keyword, user);
				table_art.add(cell);
			}
			input = new Scanner(new FileReader("R:\\Server-file\\logs.txt"));
			while (input.hasNextLine()) {
				String name = input.next();
				table_log.put(name, input.next());
			}
		} catch (FileNotFoundException e) {
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		PrintWriter out = response.getWriter();

		// HTML форма, отправляемая методом post
		show_welcome(out);
		out.close();
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.setContentType(CONTENT_TYPE);

			PrintWriter out = response.getWriter();
			HttpSession session = request.getSession(false);
			String uri = request.getRequestURI();

			out.println("<html>");
			out.println("<head><title>Catalog</title></head>");
			out.println("<body background=\"https://www.artleo.com/pic/201108/1680x1050/artleo.com-7982.jpg\">");

			if (uri.equals("/Web-catalog/Catalog/Sign") && session != null) {
				session.invalidate();
			}
			String user;

			if (session == null) {
				session = request.getSession();
				session.setAttribute("Auth", auth);
				user = request.getParameter("User");
				session.setAttribute("User", user);
			} else {
				auth = (int) session.getAttribute("Auth");
				user = (String) session.getAttribute("User");
			}

			if (auth == -1) {
				String pass = request.getParameter("Password");
				for (Map.Entry<String, String> entry : table_log.entrySet()) {
					if (user.equals(entry.getKey()) && pass.equals(entry.getValue())) {
						auth = 1;
						session.setAttribute("Auth", auth);
					}
				}
				if (auth == -1)
					show_err_log(out);
			}

			if ((int) session.getAttribute("Auth") == 1) {

				for (Article entry : table_art) {
					if (uri.equals("/Web-catalog/Catalog/Pro" + entry.getNumber())) {
						show_properties(out, entry.getNumber(), session);
					}
					if (uri.equals("/Web-catalog/Catalog/Pro" + entry.getNumber())) {
						File file = new File("R:\\Server-file\\name_" + entry.getNumber() + ".pdf");
						file.delete();
						table_art.remove(table_art.indexOf(entry));
					}
				}

				out.println("<p align=\"right\"> " + user + "</p>");
				out.println(" <form method=\"post\" action=\"/Web-catalog/Catalog/Logout\">");
				out.println("<p align=\"right\"><input type=\"submit\" value=\"Logout\"> </p></form> ");

				if (uri.equals("/Web-catalog/Catalog/Sign")) {

					show_Log(out);

				} else if (uri.equals("/Web-catalog/Catalog/Upload")) {

					Upload(out);

				} else if (uri.equals("/Web-catalog/Catalog/Upload_file")) {

					out.println("<p align=\"center\">Upload Article</p>");
					Upload_art(out);
					int number = -1;
					ArrayList<Integer> degree = new ArrayList<>();
					for (Article entry : table_art) {
						degree.add(entry.getNumber());
					}
					for (int i = 0; i < 255; i++) {
						if (!degree.contains(i)) {
							number = i;
							break;
						}
					}

					FileOutputStream fos = new FileOutputStream("R:\\Server-file\\name_" + number + ".pdf");
					int[] dataSlice = extractData(request);
					int i;
					for (i = 0; i < dataSlice.length; i++)
						fos.write(dataSlice[i]);
					fos.flush();
					fos.close();
					session.setAttribute("Number", number);

				} else if (uri.equals("/Web-catalog/Catalog/Upload_cell")) {

					String Author = request.getParameter("Author");
					String Theme = request.getParameter("Theme");
					String year = request.getParameter("Year");
					ArrayList<String> keyword = new ArrayList<>();
					for (int i = 0; i < 5; i++) {
						keyword.add(request.getParameter("Key" + i));
					}
					Article cell = new Article((int) session.getAttribute("Number"), Author, Theme,
							Integer.parseInt(year), keyword, (String) session.getAttribute("User"));
					table_art.add(cell);

				} else if (uri.equals("/Web-catalog/Catalog/Upload_fin")) {

					out.println("<p align=\"center\"> Article is uploaded</p>");
					show_Log(out);

				} else if (uri.equals("/Web-catalog/Catalog/Logout")) {

					session.invalidate();
					session.setAttribute("Auth", -1);
					show_welcome(out);
				} else if (uri.equals("/Web-catalog/Catalog/search")) {

					String key = request.getParameter("key_search");
					ArrayList<Integer> mas = new ArrayList<>();
					for (Article entry : table_art) {
						if (entry.getForEdit().contains(key)) {
							mas.add(entry.getNumber());
						}
					}

					try {
						out.println("<table cols=\"4\" border=\"0\" width=\"90%\" cellpadding=\"5\" align=\"center\">");
						for (int j = 0; j < (mas.size() + 4) / 4; j++) {
							out.println("<tr align=\"center\">");
							for (int i = 0; i < 4; i++) {
								for (Article entry : table_art) {
									if (entry.getNumber() == mas.get(i + 4 * j)) {
										Article cur = entry;
										out.println("<td>" + cur.getForCell()
												+ "<form method=\"post\" action=\"/Web-catalog/Catalog/Pro"
												+ cur.getNumber() + "\">"
												+ "<input type=\"submit\" value=\"Properties\"> </form></td>");
									}
								}
							}
							out.println("</tr>");
						}
						out.println("</table>");
					} catch (Exception e) {
					}

				}
			} else {
				show_Ent(out);
			}

			out.println("</body></html>");
			out.close();
		} catch (Exception e) {
		}
	}

	private int[] extractData(HttpServletRequest request) throws IOException {
		InputStream is = request.getInputStream();
		int[] data = new int[request.getContentLength()];
		int bytes;
		int counter = 0;
		while ((bytes = is.read()) != -1) {
			data[counter] = bytes;
			counter++;
		}
		is.close();

		// Определение индексов срезки
		int i;
		int beginSliceIndex = 0;
		int endSliceIndex = data.length - getBoundary(request).length() - 9;

		for (i = 0; i < data.length; i++) {
			if (data[i] == CR && data[i + 1] == LF && data[i + 2] == CR && data[i + 3] == LF) {
				beginSliceIndex = i + 4;
				break;
			}
		}

		int[] dataSlice = new int[endSliceIndex - beginSliceIndex + 1];
		for (i = beginSliceIndex; i <= endSliceIndex; i++) {
			dataSlice[i - beginSliceIndex] = data[i];
		}

		return dataSlice;
	}

	private String getBoundary(HttpServletRequest request) {
		String cType = request.getContentType();
		return cType.substring(cType.indexOf("boundary=") + 9);
	}

	private void show_Ent(PrintWriter out) {

		ArrayList<String> cells = new ArrayList<>();
		int c = 0;
		for (Article entry : table_art) {
			cells.add(entry.getForCell());
			c++;
		}

		out.println("<table cols=\"4\" border=\"0\" width=\"90%\" cellpadding=\"5\" align=\"center\">");
		for (int j = 0; j < c + 3 / 4; j++) {
			out.println("<tr align=\"center\">");
			for (int i = 0; i < 4; i++) {
				out.println("<td>" + table_art.get(i + 4 * j).getForCell() + "<form action=\"/Web-catalog/Catalog/"
						+ table_art.get(i + 4 * j).getNumber() + "\">"
						+ "    <input type=\"submit\" value=\"Properties\"> </form></td>");
			}
			out.println("</tr>");
		}
		out.println("</table>");

		out.println("<div class=\"search\">");
		out.println("<form action=\"/Catalog/search\" method=\"post\">");
		out.println(
				"<p	align=\"center\">Author, theme, year or keyword : <input type=\"text\" name=\"key_search\" required></p>");
		out.println("<p	align=\"center\">  <input type=\"submit\" value=\"Search\"></p>");
		out.println("</form></div>");

	}

	private void show_Log(PrintWriter out) { // search + delete + edit

		out.println("<form action=\"/Web-catalog/Catalog/Upload\" method=\"post\">");
		out.println("<p	align=\"center\">  <input type=\"submit\" value=\"Upload article\"></p>");
		out.println("</form>");

		ArrayList<String> cells = new ArrayList<>();
		int c = 0;
		for (Article entry : table_art) {
			cells.add(entry.getForCell());
			c++;
		}

		out.println("<form action=\"/Web-catalog/Catalog/search\" method=\"post\">");
		out.println(
				"<p	align=\"center\">Author, theme, year or keyword : <input type=\"text\" name=\"key_search\" required></p>");
		out.println("<p	align=\"center\">  <input type=\"submit\" value=\"Search\"></p>");
		out.println("</form>");

		out.println("<table cols=\"4\" border=\"0\" width=\"90%\" cellpadding=\"5\" align=\"center\">");
		for (int j = 0; j < c + 1 / 4; j++) {
			out.println("<tr align=\"center\">");
			for (int i = 0; i < 4; i++) {
				out.println("<td>" + table_art.get(i + 4 * j).getForCell() + "<form action=\"/Web-catalog/Catalog/Pro"
						+ table_art.get(i + 4 * j).getNumber() + "\">"
						+ "<input type=\"submit\" value=\"Properties\"> </form></td>");
			}
			out.println("</tr>");
		}
		out.println("</table>");
	}

	private void Upload(PrintWriter out) {

		out.println("<div class=\"upload\">");
		out.println(
				"<form action=\"/Web-catalog/Catalog/Upload_file\" method=\"post\" enctype=\"multipart/form-data\">");

		out.println("<p align=\"center\">Upload Article</p>");
		out.println("<p align=\"center\" ><input  type=file name=ufile multiple accept=\"application/pdf\">");
		out.println("<input type=submit value=\"Attach\"></p>");
		out.println("</form></div>");
	}

	private void Upload_art(PrintWriter out) {
		out.println("<form action=\"/Web-catalog/Catalog/Properties_fin\" method=\"post\">");
		out.println(
				"<p align=\"center\"> Author : <input type=\"text\" name=\"Author\" placeholder=\"Author\" size=\"10\" required> </p>");
		out.println(
				"<p align=\"center\">Theme : <input type=\"text\" name=\"Theme\" placeholder=\"Theme\" size=\"10\"	required></p>");
		out.println(
				"<p align=\"center\">Year : <input type=\"number\" name=\"year\" min=\"1\" max=\"3000\" value=\"1\" placeholder=\"Year\" size=\"10\" required></p>");
		out.println("<p align=\"center\">Keyword : <input type=\"text\" name=\"key1\" size=\"10\" required>-<input");
		out.println(
				"type=\"text\" name=\"key2\" size=\"10\">-<input type=\"text\" name=\"key3\" size=\"10\">-<input type=\"text\" name=\"key4\"");
		out.println("size=\"10\">-<input type=\"text\" name=\"key5\" size=\"10\">	</p>");
		out.println("<p align=\"center\" > <input type=submit value=\"Attach\" ></p>");
		out.println("</form>");
	}

	private void show_properties(PrintWriter out, int number, HttpSession session) {
		out.println("<form action=\"/Web-catalog/Catalog/Prop_Edited\" method=\"post\">");
		for (Article entry : table_art)
			if (entry.getUser() == session.getAttribute("User"))
				if (entry.getNumber() == number) {
					out.println("<p align=\"center\"> " + "Author : <input type=\"text\" name=\"Author\" placeholder=\""
							+ entry.getAuthor() + "\" size=\"10\" required> </p>");
					out.println("<p align=\"center\">" + "Theme : <input type=\"text\" name=\"Theme\" placeholder=\""
							+ entry.getTheme() + "\" size=\"10\"	required></p>");
					out.println("<p align=\"center\">"
							+ "Year : <input type=\"number\" name=\"year\" min=\"1\" max=\"3000\" value=\""
							+ entry.getYear() + "\"  size=\"10\" required></p>");
					out.println("<p align=\"center\" > <input type=submit value=\"Edit\" ></p>");
					out.println("</form>");
					out.println(
							"<form method=\"post\" action=\"/Web-catalog/Catalog/Delete" + entry.getNumber() + "\">");
					out.println("<p align=\"center\"><input type=\"submit\" value=\"Delete Article\"></p>");
					out.println("</form>");
				}

	}

	private void show_err_log(PrintWriter out) {
		out.println("<form action=\"/Web-catalog/Catalog/Sign\" method=\"post\">");
		out.println("<p align=\"center\">Sorry, incorrect login or password</p>");
		out.println("<p align=\"center\">Please, enter correct login and password</p>");
		out.println("<p align=\"center\">");
		out.println("<input type=\"text\" name=\"login\" placeholder=\"User\" required>");
		out.println("<input type=\"password\" name=\"Password\" placeholder=\"Password\">");
		out.println("<input type=\"submit\" value=\"Sign in\"></p>");
		out.println("</form>");
	}

	private void show_welcome(PrintWriter out) {
		out.println("<!DOCTYPE html><html>");
		out.println("<body background=\"http://images.hdwallpaperpics.net/5286b6d2829d277380.jpg\">");
		out.println("<meta charset=\"UTF-8\"> <title>Welcome!</title> </head> <body "
				+ "background=\"http://images.hdwallpaperpics.net/5286b6d2829d277380.jpg\"> <p align=\"center\"> Welcome! </p>"
				+ "<p align=\"center\">The library is waiting for you!</p><form action=\"/Web-catalog/Catalog/Sign\" method=\"post\">"
				+ "<p align=\"right\"><input type=\"text\" name=\"User\" placeholder=\"Login\" required><input "
				+ "type=\"password\" name=\"Password\" placeholder=\"Password\" required> <input type=\"submit\" value=\"Sign in\">"
				+ "</p></form><form action=\"/Web-catalog/Catalog/Ent\" method=\"post\"><p align=\"right\">"
				+ "<input type=\"submit\" value=\"Guest\"></p></form>");
		out.println("</body></html>");
	}
	
}
