package edu.umich.eecs.tac.props;
import java.io.Serializable;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import se.sics.isl.transport.TransportReader;
import se.sics.isl.transport.TransportWriter;
import se.sics.isl.transport.Transportable;


public class KeywordReport implements Serializable, Transportable {

	/**Update when this file is updated.
	 * 
	 */
	private static final long serialVersionUID = -4366560152538990286L;

	private List<QueryReport> yesterdaysReport;
	private boolean isLocked = false;
	
	public KeywordReport(){
		yesterdaysReport = new LinkedList<QueryReport>();
	}
	
	public boolean isLocked() {
		return isLocked;
	}

	public void lock() {
		isLocked = true;
	}
	
	public String getTransportName() {
		return "keywordreport";
	}

	public String toString(){
		String r = new String();
		int i;
		QueryReport temp = new QueryReport();
		for(i = 0; i < yesterdaysReport.size(); i++){
			temp = yesterdaysReport.get(i);
			r += "Query: " + temp.getQueryString() + " ";
			r += "Position: " + temp.getPosition() + " ";
			r += "Impressions: " + temp.getImpressions() + " ";
			r += "Clicks: " + temp.getClicks() + " ";
			r += "Conversions: " + temp.getConversions() + " ";
			r += "Cost: " + temp.getCost() + " ";
			r += "Revenue: " + temp.getRevenue();
			r += "\n";
		}
		return r;
	}
	
	public void read(TransportReader reader) throws ParseException {
	    if (isLocked) {
	        throw new IllegalStateException("locked");
	    }
		boolean lock = reader.getAttributeAsInt("lock", 0) > 0;
		while (reader.nextNode("keywordreport", false)) {
			QueryReport temp = new QueryReport();
			temp.setQueryString(reader.getAttribute("queryString"));
			temp.setImpressions(reader.getAttributeAsInt("impressions"));
			temp.setClicks(reader.getAttributeAsInt("clicks"));
			temp.setConversions(reader.getAttributeAsInt("conversions"));
			temp.setPosition(reader.getAttributeAsInt("position"));
			temp.setCost(reader.getAttributeAsFloat("cost"));
			temp.setRevenue(reader.getAttributeAsFloat("revenue"));
			yesterdaysReport.add(temp);
		}
	    if(lock){
	        lock();
	    }
	}

	public void write(TransportWriter writer) {
		if (isLocked) {
			writer.attr("lock", 1);
	    }
		int i;
		QueryReport temp;
		for(i = 0; i < yesterdaysReport.size(); i++){
			temp = yesterdaysReport.get(i);
			writer.node("keywordreport");
			writer.attr("queryString", temp.getQueryString());
			writer.attr("impressions", temp.getImpressions());
			writer.attr("clicks", temp.getClicks());
			writer.attr("conversions", temp.getConversions());
			writer.attr("position", temp.getPosition());
			writer.attr("cost", temp.getCost());
			writer.attr("revenue", temp.getRevenue());
			writer.endNode("keywordreport");
		}
	}

	private static class QueryReport {

		private String queryString = new String();
		private int impressions;
		private int clicks;
		private int conversions;
		private int position;
		private float cost;
		private float revenue;
		
		public QueryReport(){}
		
		public QueryReport(String s){
			queryString = s;
		}
		
		public QueryReport(String s, int cli, int con, float cos, int imp, int pos, float rev){
			queryString = s;
			clicks = cli;
			conversions = con;
			cost = cos;
			impressions = imp;
			position = pos;
			revenue = rev;
		}
		
		public String getQueryString() {
			return queryString;
		}
		
		public void setQueryString(String q) {
			queryString = q;
		}
		
		public int getImpressions() {
			return impressions;
		}
		public void setImpressions(int impressions) {
			this.impressions = impressions;
		}
		public int getClicks() {
			return clicks;
		}
		public void setClicks(int clicks) {
			this.clicks = clicks;
		}
		public int getConversions() {
			return conversions;
		}
		public void setConversions(int conversions) {
			this.conversions = conversions;
		}
		public int getPosition() {
			return position;
		}
		public void setPosition(int position) {
			this.position = position;
		}
		public float getCost() {
			return cost;
		}
		public void setCost(float cost) {
			this.cost = cost;
		}
		public float getRevenue() {
			return revenue;
		}
		public void setRevenue(float revenue) {
			this.revenue = revenue;
		}
		
	}
	
}