import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.jfree.data.time.Second;
import java.util.LinkedList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;


public class Metrics {

	public void generateTimeLineForTweets() {
		generateTimeLine(EnumDataSet.TWEETS);
	}
	
	public void generateTimeLineForReTweets() {
		generateTimeLine(EnumDataSet.RETWEETS);
	}
	
	/**
	 * Generates timeline for tweet volume
	 * */
	private void generateTimeLine(EnumDataSet workingSet) {
		Calendar startTime = Calendar.getInstance();
		startTime.set(2013, 2, 2, 22, 36, 0);
		startTime.set(Calendar.MILLISECOND, 0);

		Calendar endTime = Calendar.getInstance();
		endTime.set(2013, 2, 21, 16, 10, 0);
		endTime.set(Calendar.MILLISECOND, 0);
		
		Calendar currTime = Calendar.getInstance();
		currTime.setTimeInMillis(startTime.getTimeInMillis());
		currTime.add(Calendar.MINUTE, 10);
		
		int count = 0;
		while (startTime.before(endTime)) {
			Set<String> uniqueUsers = new HashSet<String>();
			ResultSet rs = QueryDB.getDataInInterval(workingSet, startTime, currTime);
			try {
				while (rs.next()) {
					uniqueUsers.add(rs.getString(3));
					count++;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			Store.updateInInterval(workingSet, startTime, currTime, count, uniqueUsers.size());
			
			startTime.setTimeInMillis(currTime.getTimeInMillis());
			currTime.add(Calendar.MINUTE, 10);
			count = 0;
		}
		
	}
	
	private void createChart(LinkedList<ReTweetInInterval> retweetsList ){

		int i = 0;
		TimeSeries c = new TimeSeries("Retweet frequency", Second.class);
		for (ReTweetInInterval reTweetInInterval: retweetsList) {
			if (i != 0) {
				Second regularTimePeriod =  new Second(reTweetInInterval.getEnd());

				c.add(regularTimePeriod, reTweetInInterval.getNoOfReTweets());
			}
			i++;
		}

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(c);
		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				"retweet frequency",
				"Date",
				"Population",
				dataset,
				true,
				true,
				false);
		try {
			ChartUtilities.saveChartAsJPEG(new File("chart.jpg"), chart, 500, 300);
		} catch (IOException e) {
			System.err.println("Problem occurred creating chart.");
		}

	}

	/**
	 * BUILD timeline_Table (hashmap) (take String[] users, Calendar start, Calendar end, min interval)
	 * */
	public void buildTimelineforUsers(String[] users, Calendar start, Calendar end, int intervalMin) {
		ResultSet rs = QueryDB.getRetweetsForUsersInInterval(users, start, end);
		int i = 1;
		Calendar currTime = Calendar.getInstance();

		Calendar beginInterval = Calendar.getInstance();
		beginInterval.setTimeInMillis(start.getTimeInMillis());
		Calendar endInterval = Calendar.getInstance();
		endInterval.setTimeInMillis(beginInterval.getTimeInMillis());
		endInterval.add(Calendar.MINUTE, intervalMin);

		LinkedList<ReTweetInInterval> retweetsList = new LinkedList<ReTweetInInterval>();
		int reTweetCount = 0;
		ReTweetInInterval prevTweet = new ReTweetInInterval("0", beginInterval, endInterval);
		ReTweetInInterval curTweet = new ReTweetInInterval("0", beginInterval, endInterval);
		// make beginInterval = currTime and keep moving, when there is a jump in currTime, fill in the middle stuff with crap
		try {
			while (rs.next()) {
				// Get time of current row
				currTime.setTimeInMillis(rs.getTimestamp(2).getTime());
				// Create current Tweet
				curTweet = new ReTweetInInterval(rs.getString(5), beginInterval, endInterval);
				// Still in same set of reTweet
				if (prevTweet.equals(rs.getString(5)) && (endInterval.after(currTime) || endInterval.equals(currTime))) {
					System.out.println("counting");

					reTweetCount++;
				} else if (prevTweet.equals(rs.getString(5)) && endInterval.before(currTime)) {
					System.out.println("changed time interval");
					System.out.println(i);
					i++;

					prevTweet.setNoOfReTweets(reTweetCount);
					retweetsList.add(prevTweet);
					reTweetCount = 1;
					//					beginInterval.setTimeInMillis(endInterval.getTimeInMillis());
					//					endInterval.add(Calendar.MINUTE, intervalMin);

					beginInterval.setTimeInMillis(currTime.getTimeInMillis());
					endInterval.setTimeInMillis(currTime.getTimeInMillis());
					endInterval.add(Calendar.MINUTE, intervalMin);
				} else {
					System.out.println("changed tweet");
					prevTweet.setNoOfReTweets(reTweetCount);
					retweetsList.add(prevTweet);
					reTweetCount = 1;
					beginInterval.setTimeInMillis(start.getTimeInMillis());
					endInterval.setTimeInMillis(beginInterval.getTimeInMillis());
					endInterval.add(Calendar.MINUTE, intervalMin);
				}
				// Create previous tweet
				prevTweet = new ReTweetInInterval(rs.getString(5), beginInterval, endInterval);
				//				System.out.println("Tweet No: " + i);
				//				System.out.println("TweetID: " + rs.getString(1));
				//				System.out.println("DateTime: " + rs.getDate(2).toString() + " " + rs.getTime(2).toString());
				//				System.out.println("Calendar dateTime: " + currTime.getTime().toString());
				//				System.out.println("Original TweetID: " + rs.getString(5));
				//				System.out.println();
				//				i++;
			}
			prevTweet.setNoOfReTweets(reTweetCount);
			retweetsList.add(prevTweet);
		} catch (SQLException e) {
			e.printStackTrace();
		}


		for (ReTweetInInterval reTweetInInterval: retweetsList) {
			System.out.println(reTweetInInterval.toString());
		} 


		createChart(retweetsList);
	}
	//add stuff to hashmap


	// Metric for a tweet
	// Metric for a user
	// Metric taking into account followers/followees
	// How metrics change over time per user, per tweet etc
	// show a user's influence at time x, then time y, etc...
}
