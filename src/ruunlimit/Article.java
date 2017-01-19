package ruunlimit;

import java.util.ArrayList;

public class Article {
	private int year, number;
	private String author, theme, user;
	private ArrayList<String> keyword;

	public Article(int number, String author, String theme, int year, ArrayList<String> keyword, String user) {
		this.year = year;
		this.user = user;
		this.number = number;
		this.author = author;
		this.theme = theme;
		this.keyword = keyword;
	}

	public String getForCell() {
		return "<p>" + theme + "</p><p>" + author + "</p>";
	}
	
	public String getUser(){
		return user;
	}
	
	public int getNumber(){
		return number;
	}
	
	public String getAuthor(){
		return author;
	}
	
	public int getYear(){
		return year;
	}
	
	public String getTheme(){
		return theme;
	}
	
	public String getForEdit() {
		return theme + " " + author + " " + year + " " + keyword.toString();
	}
	
	public void setCell(String author, String theme){
		this.author = author;
		this.theme = theme;
	}

}
