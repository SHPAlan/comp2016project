import java.awt.GridLayout;
import java.awt.TextField;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import javax.swing.*;

import java.util.Date;
import java.util.Properties;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * This is a flight manager to support: (1) add a flight (2) delete a flight (by
 * flight_no) (3) print flight information (by flight_no) (4) select a flight
 * (by source, dest, stop_no = 0) (5) select a flight (by source, dest, stop_no
 * = 1)
 * 
 * @author comp1160/2016
 */

public class FlightManager {

	Scanner in = null;
	Connection conn = null;
	// Database Host
	final String databaseHost = "orasrv1.comp.hkbu.edu.hk";
	// Database Port
	final int databasePort = 1521;
	// Database name
	final String database = "pdborcl.orasrv1.comp.hkbu.edu.hk";
	final String proxyHost = "faith.comp.hkbu.edu.hk";
	final int proxyPort = 22;
	final String forwardHost = "localhost";
	int forwardPort;
	Session proxySession = null;
	boolean noException = true;

	// JDBC connecting host
	String jdbcHost;
	// JDBC connecting port
	int jdbcPort;

	String[] options = { // if you want to add an option, append to the end of
							// this array
			"add a flight", "delete a flight (by flight_no)", "print flight information (by flight_no)", 
			"select a flight (by source, dest, stop_no = 0)", "select a flight (by source, dest, stop_no = 1)",
			"book a flight (by customer_id, flight_no)", "cancel a flight (by customer_id, booking_id)", "exit"};

	/**
	 * Get YES or NO. Do not change this function.
	 * 
	 * @return boolean
	 */
	boolean getYESorNO(String message) {
		JPanel panel = new JPanel();
		panel.add(new JLabel(message));
		JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
		JDialog dialog = pane.createDialog(null, "Question");
		dialog.setVisible(true);
		boolean result = JOptionPane.YES_OPTION == (int) pane.getValue();
		dialog.dispose();
		return result;
	}

	/**
	 * Get username & password. Do not change this function.
	 * 
	 * @return username & password
	 */
	String[] getUsernamePassword(String title) {
		JPanel panel = new JPanel();
		final TextField usernameField = new TextField();
		final JPasswordField passwordField = new JPasswordField();
		panel.setLayout(new GridLayout(2, 2));
		panel.add(new JLabel("Username"));
		panel.add(usernameField);
		panel.add(new JLabel("Password"));
		panel.add(passwordField);
		JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
			private static final long serialVersionUID = 1L;

			@Override
			public void selectInitialValue() {
				usernameField.requestFocusInWindow();
			}
		};
		JDialog dialog = pane.createDialog(null, title);
		dialog.setVisible(true);
		dialog.dispose();
		return new String[] { usernameField.getText(), new String(passwordField.getPassword()) };
	}

	/**
	 * Login the proxy. Do not change this function.
	 * 
	 * @return boolean
	 */
	public boolean loginProxy() {
		if (getYESorNO("Using ssh tunnel or not?")) { // if using ssh tunnel
			String[] namePwd = getUsernamePassword("Login cs lab computer");
			String sshUser = namePwd[0];
			String sshPwd = namePwd[1];
			try {
				proxySession = new JSch().getSession(sshUser, proxyHost, proxyPort);
				proxySession.setPassword(sshPwd);
				Properties config = new Properties();
				config.put("StrictHostKeyChecking", "no");
				proxySession.setConfig(config);
				proxySession.connect();
				proxySession.setPortForwardingL(forwardHost, 0, databaseHost, databasePort);
				forwardPort = Integer.parseInt(proxySession.getPortForwardingL()[0].split(":")[0]);
			} catch (JSchException e) {
				e.printStackTrace();
				return false;
			}
			jdbcHost = forwardHost;
			jdbcPort = forwardPort;
		} else {
			jdbcHost = databaseHost;
			jdbcPort = databasePort;
		}
		return true;
	}

	/**
	 * Login the oracle system. Change this function under instruction.
	 * 
	 * @return boolean
	 */
	public boolean loginDB() {
		String username = "e9229895";//Replace e1234567 to your username
		String password = "e9229895";//Replace e1234567 to your password
		
		/* Do not change the code below */
		if(username.equalsIgnoreCase("e1234567") || password.equalsIgnoreCase("e1234567")) {
			String[] namePwd = getUsernamePassword("Login sqlplus");
			username = namePwd[0];
			password = namePwd[1];
		}
		String URL = "jdbc:oracle:thin:@" + jdbcHost + ":" + jdbcPort + "/" + database;

		try {
			System.out.println("Logging " + URL + " ...");
			conn = DriverManager.getConnection(URL, username, password);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Show the options. If you want to add one more option, put into the
	 * options array above.
	 */
	public void showOptions() {
		System.out.println("Please choose following option:");
		for (int i = 0; i < options.length; ++i) {
			System.out.println("(" + (i + 1) + ") " + options[i]);
		}
	}

	/**
	 * Run the manager
	 */
	public void run() {
		while (noException) {
			showOptions();
			String line = in.nextLine();
			if (line.equalsIgnoreCase("exit"))
				return;
			int choice = -1;
			try {
				choice = Integer.parseInt(line);
			} catch (Exception e) {
				System.out.println("This option is not available");
				continue;
			}
			if (!(choice >= 1 && choice <= options.length)) {
				System.out.println("This option is not available");
				continue;
			}
			if (options[choice - 1].equals("add a flight")) {
				addFlight();
			} else if (options[choice - 1].equals("delete a flight (by flight_no)")) {
				deleteFlight();
			} else if (options[choice - 1].equals("print flight information (by flight_no)")) {
				printFlightByNo();
			} else if (options[choice - 1].equals("select a flight (by source, dest, stop_no = 0)")) {
				selectFlightsInZeroStop();
			} else if (options[choice - 1].equals("select a flight (by source, dest, stop_no = 1)")) {
				selectFlightsInOneStop();
			} else if (options[choice - 1].equals("book a flight (by customer_id, flight_no)")) {
				bookFlight();
			} else if (options[choice - 1].equals("cancel a flight (by customer_id, booking_id)")) {
				cancelFlight();
			} else if (options[choice - 1].equals("exit")) {
				break;
			}
		}
	}

	/**
	 * Print out the infomation of a flight given a flight_no
	 * 
	 * @param flight_no
	 */
	private void printFlightInfo(String flight_no) {
		try {
			Statement stm = conn.createStatement();
			String sql = "SELECT * FROM FLIGHTS WHERE Flight_no = '" + flight_no + "'";
			ResultSet rs = stm.executeQuery(sql);
			if (!rs.next())
				return;
			String[] heads = { "Flight_no", "Depart_Time", "Arrive_Time", "Fare", "Seat_Limit", "Source", "Dest" };
			for (int i = 0; i < 7; ++i) { // flight table 7 attributes
				try {
					System.out.println(heads[i] + " : " + rs.getString(i + 1)); // attribute
																				// id
																				// starts
																				// with
																				// 1
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			noException = false;
		}
	}

	/**
	 * List all flights in the database.
	 */
	private void listAllFlights() {
		System.out.println("All flights in the database now:");
		try {
			Statement stm = conn.createStatement();
			String sql = "SELECT Flight_No FROM FLIGHTS";
			ResultSet rs = stm.executeQuery(sql);

			int resultCount = 0;
			while (rs.next()) {
				System.out.println(rs.getString(1));
				++resultCount;
			}
			System.out.println("Total " + resultCount + " flight(s).");
			rs.close();
			stm.close();
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
	}

	/**
	 * Select out a flight according to the flight_no.
	 */
	private void printFlightByNo() {
		listAllFlights();
		System.out.println("Please input the flight_no to print info:");
		String line = in.nextLine();
		line = line.trim();
		if (line.equalsIgnoreCase("exit"))
			return;

		printFlightInfo(line);
	}

	/**
	 * Given source and dest, select all the flights can arrive the dest
	 * directly. For example, given HK, Tokyo, you may find HK -> Tokyo Your job
	 * to fill in this function.
	 */
	private void selectFlightsInZeroStop() {
		System.out.println("Please input source, dest:");

		String line = in.nextLine();

		if (line.equalsIgnoreCase("exit"))
			return;

		String[] values = line.split(",");
		for (int i = 0; i < values.length; ++i)
			values[i] = values[i].trim();

		try {
			/**
			 * Create the statement and sql
			 */
			Statement stm = conn.createStatement();

			String sql = "";

			/**
			 * Formulate your own SQL query:
			 *
			 * sql = "...";
			 *
			 */
			System.out.println(sql);

			ResultSet rs = stm.executeQuery(sql);

			int resultCount = 0; // a counter to count the number of result
									// records
			while (rs.next()) { // this is the result record iterator, see the
								// tutorial for details

				/*
				 * Write your own to print flight information; you may use the
				 * printFlightInfo() function
				 */

			}
			System.out.println("Total " + resultCount + " choice(s).");
			rs.close();
			stm.close();
		} catch (SQLException e) {
			e.printStackTrace();
			noException = false;
		}
	}

	/**
	 * Given source and dest, select all the flights can arrive the dest in one
	 * stop. For example, given HK, Tokyo, you may find HK -> Beijing, Beijing
	 * -> Tokyo Your job to fill in this function.
	 */
	private void selectFlightsInOneStop() {
		System.out.println("Please input source, dest:");

		String line = in.nextLine();

		if (line.equalsIgnoreCase("exit"))
			return;

		String[] values = line.split(",");
		for (int i = 0; i < values.length; ++i)
			values[i] = values[i].trim();

		/**
		 * try {
		 * 
		 * // Similar to the 'selectFlightsInZeroStop' function; write your own
		 * code here
		 * 
		 * 
		 * } catch (SQLException e) { e.printStackTrace(); noException = false;
		 * }
		 */
	}

	/**
	 * Insert data into database
	 * 
	 * @return
	 */
	private void addFlight() {
		/**
		 * A sample input is: CX109, 2015/03/15/13:00:00, 2015/03/15/19:00:00,
		 * 2000, Beijing, Tokyo
		 */
		System.out.println("Please input the flight_no, depart_time, arrive_time, fare, seat_limit, source, dest:");
		String line = in.nextLine();

		if (line.equalsIgnoreCase("exit"))
			return;
		String[] values = line.split(",");

		if (values.length < 7) {
			System.out.println("The value number is expected to be 7");
			return;
		}
		for (int i = 0; i < values.length; ++i)
			values[i] = values[i].trim();

		try {
			Statement stm = conn.createStatement();
			String sql = "INSERT INTO FLIGHTS VALUES(" + "'" + values[0] + "', " + // this
																					// is
																					// flight
																					// no
			"to_date('" + values[1] + "', 'yyyy/mm/dd/hh24:mi:ss'), " + // this
																		// is
																		// depart_time
			"to_date('" + values[2] + "', 'yyyy/mm/dd/hh24:mi:ss'), " + // this
																		// is
																		// arrive_time
			values[3] + ", " + // this is fare
			values[4] + ", " + // this is seat_limit
			"'" + values[5] + "', " + // this is source
			"'" + values[6] +  // this is dest
			"')";
			stm.executeUpdate(sql);
			stm.close();
			System.out.println("succeed to add flight ");
			printFlightInfo(values[0]);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("fail to add a flight " + line);
			noException = false;
		}
	}

	/**
	 * Please fill in this function to delete a flight.
	 */
	public void deleteFlight() {
		listAllFlights();
		System.out.println("Please input the flight_no to delete:");
		String line = in.nextLine();

		if (line.equalsIgnoreCase("exit"))
			return;
		line = line.trim();

		try {
			Statement stm = conn.createStatement();

			String sql = "DELETE FROM FLIGHTS WHERE FLIGHT_NO = '"  + line + "'";

			stm.executeUpdate(sql); // please pay attention that we use
									// executeUpdate to update the database
			stm.close();

			System.out.println("succeed to delete flight " + line);
			 
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("fail to delete flight " + line);
			noException = false;
		}
	}
	
	public void bookFlight() {
		System.out.println("Please input the customer_id:");
		String customerID = in.nextLine();
		
		System.out.println("Please input the flight numbers(separate by ,):");
		String[] flightNo = in.nextLine().split(",");
		
		if (flightNo.length > 3) {
			System.out.println("A connection of more than 3 flights is not allowed.");
			System.out.println();
			return;
		}
		
		
		for (int i = 0; i < flightNo.length; ++i) {
			flightNo[i] = flightNo[i].trim();
		}
		
		try {
			Statement stm = conn.createStatement();
			String sql = "SELECT COUNT(*) FROM BOOKING"; // find the no. of bookings
			ResultSet rs1 = stm.executeQuery(sql);
			
			int bookingNo = 0;
			
			while (rs1.next()) {
				bookingNo = rs1.getInt(1);
			}
			
			rs1.close();
			
			// check if all of the flights have seats
			for (int i = 0; i < flightNo.length; i++) {
				sql = "SELECT SEAT_LIMIT FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[i] + "'";
				ResultSet rs2 = stm.executeQuery(sql);
				
				while (rs2.next()) {
					if (rs2.getInt(1) <= 0) {
						System.out.println("Seats full.");
						System.out.println();
						return;
					}
				}
				rs2.close();
			}
			
			if (flightNo.length == 1) {
				bookingNo++;
				sql = "INSERT INTO BOOKING VALUES('" + customerID + "', " + "'" + flightNo[0] 
					+ "', " + "'B" + bookingNo + "')";
				stm.executeUpdate(sql);
				System.out.println("Succeed to book a flight for " + customerID + 
						           ", flight id is B" + bookingNo);
				System.out.println();
			} else {
				if (flightNo.length == 2) {
					String dest = "";
					String source = "";
					
					sql = "SELECT DEST FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[0] + "'";
					ResultSet rs3 = stm.executeQuery(sql);
					
					while (rs3.next()) {
						dest = rs3.getString(1);
					}
					
					rs3.close();
					
					sql = "SELECT SOURCE FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[1] + "'";
					ResultSet rs4 = stm.executeQuery(sql);
					
					while (rs4.next()) {
						source = rs4.getString(1);
					}
					
					rs4.close();
					
					if (!dest.equals(source)) {
						System.out.println("The destination of " + flightNo[0] + " does not "
								+ "coincide with the source of " + flightNo[1] + ".");
						System.out.println();
						return;
					}
					
					//SimpleDateFormat sdf = new SimpleDateFormat("yyyy/mm/dd/hh:mm:ss");
					Date arrive = null, depart = null;
					
					sql = "SELECT ARRIVE_TIME FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[0] + "'";
					ResultSet rs5 = stm.executeQuery(sql);
					
					while (rs5.next()) {
						arrive = rs5.getDate(1);
					}
					
					rs5.close();
					
					sql = "SELECT DEPART_TIME FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[1] + "'";
					ResultSet rs6 = stm.executeQuery(sql);
					
					while (rs6.next()) {
						depart = rs6.getDate(1);
					}
					
					rs6.close();
					
					if (arrive.after(depart)) {
						System.out.println("The arrival time of " + flightNo[0] + " is after "
								+ "the departure time of " + flightNo[1] + ".");
						System.out.println();
						return;
					}
					
                    bookingNo++;
                    
					for (int i = 0 ; i < flightNo.length; i++) {
						sql = "INSERT INTO BOOKING VALUES('" + customerID + "', " + "'" + flightNo[i] 
								+ "', " + "'B" + bookingNo + "')";
						stm.executeUpdate(sql);
					}
					
					System.out.println("Succeed to book a flight for " + customerID + 
					           ", flight id is B" + bookingNo);
					System.out.println();
					
				} else {
					String dest = "";
					String source = "";
					
					sql = "SELECT DEST FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[0] + "'";
					ResultSet rs5 = stm.executeQuery(sql);
					
					while (rs5.next()) {
						dest = rs5.getString(1);
					}
					
					rs5.close();
					
					sql = "SELECT SOURCE FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[1] + "'";
					ResultSet rs6 = stm.executeQuery(sql);
					
					while (rs6.next()) {
						source = rs6.getString(1);
					}
					
					rs6.close();
					
					if (!dest.equals(source)) {
						System.out.println("The destination of " + flightNo[0] + " does not "
								+ "coincide with the source of " + flightNo[1] + ".");
						System.out.println();
						return;
					}
					
					sql = "SELECT DEST FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[1] + "'";
					ResultSet rs7 = stm.executeQuery(sql);
					
					while (rs7.next()) {
						dest = rs7.getString(1);
					}
					
					rs7.close();
					
					sql = "SELECT SOURCE FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[2] + "'";
					ResultSet rs8 = stm.executeQuery(sql);
					
					while (rs8.next()) {
						source = rs8.getString(1);
					}
					
					rs8.close();
					
					if (!dest.equals(source)) {
						System.out.println("The destination of " + flightNo[1] + " does not "
								+ "coincide with the source of " + flightNo[2] + ".");
						System.out.println();
						return;
					}
					
                    Date arrive = null, depart = null;
					
					sql = "SELECT ARRIVE_TIME FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[0] + "'";
					ResultSet rs9 = stm.executeQuery(sql);
					
					while (rs9.next()) {
						arrive = rs9.getDate(1);
					}
					
					rs9.close();
					
					sql = "SELECT DEPART_TIME FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[1] + "'";
					ResultSet rs10 = stm.executeQuery(sql);
					
					while (rs10.next()) {
						depart = rs10.getDate(1);
					}
					
					rs10.close();
					
					if (arrive.after(depart)) {
						System.out.println("The arrival time of " + flightNo[0] + " is after "
								+ "the departure time of " + flightNo[1] + ".");
						System.out.println();
						return;
					}
				
					sql = "SELECT ARRIVE_TIME FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[1] + "'";
					ResultSet rs11 = stm.executeQuery(sql);
					
					while (rs11.next()) {
						arrive = rs11.getDate(1);
					}
					
					rs11.close();
					
					sql = "SELECT DEPART_TIME FROM FLIGHTS WHERE FLIGHT_NO = '" + flightNo[2] + "'";
					ResultSet rs12 = stm.executeQuery(sql);
					
					while (rs12.next()) {
						depart = rs12.getDate(1);
					}
					
					rs12.close();
					
					if (arrive.after(depart)) {
						System.out.println("The arrival time of " + flightNo[1] + " is after "
								+ "the departure time of " + flightNo[2] + ".");
						System.out.println();
						return;
					}
					
					bookingNo++;
					
					for (int i = 0 ; i < flightNo.length; i++) {
						sql = "INSERT INTO BOOKING VALUES('" + customerID + "', " + "'" + flightNo[i] 
								+ "', " + "'B" + bookingNo + "')";
						stm.executeUpdate(sql);
					}
					
					System.out.println("Succeed to book a flight for " + customerID + 
					           ", flight id is B" + bookingNo);
					System.out.println();
					
				}
				stm.close();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("fail to book flight");
			noException = false;
		}
	}
	
	public void cancelFlight() {
		System.out.println("Please input the customer_id and booking_id:");
		
		String line = in.nextLine();

		if (line.equalsIgnoreCase("exit")) {
			return;
		}
		
		String[] values = line.split(",");

		if (values.length < 2) {
			System.out.println("The value number is expected to be 2");
			return;
		}
		for (int i = 0; i < values.length; ++i) {
			values[i] = values[i].trim();
		}
		
		try {
			Statement stm = conn.createStatement();
			
			String sql = "SELECT COUNT(*) FROM BOOKING WHERE BOOKING_ID = '" + values[1] + "'";
			ResultSet rs = stm.executeQuery(sql);
			
			while (rs.next()) {
				if (rs.getInt(1) <= 0) {
					System.out.println("Booking " + values[1] + " customer " + values[0] + " fails to cancel");
				    System.out.println();
					return;
				}
			}
			
			rs.close();
			
			sql = "DELETE FROM BOOKING WHERE CUSTOMER_ID = '" + values[0] + "' "
					   + "AND BOOKING_ID = '" + values[1] + "'"; 
			
			stm.executeUpdate(sql);
			stm.close();
			
			System.out.println("Booking " + values[1] + " for customer " + values[0] + " is cancelled");
			System.out.println();
					   
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("fail to cancel flight " + line);
			noException = false;
		}
	}

	/**
	 * Close the manager. Do not change this function.
	 */
	public void close() {
		System.out.println("Thanks for using this manager! Bye...");
		try {
			if (conn != null)
				conn.close();
			if (proxySession != null) {
				proxySession.disconnect();
			}
			in.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructor of flight manager Do not change this function.
	 */
	public FlightManager() {
		System.out.println("Welcome to use this manager!");
		in = new Scanner(System.in);
	}

	/**
	 * Main function
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		FlightManager manager = new FlightManager();
		if (!manager.loginProxy()) {
			System.out.println("Login proxy failed, please re-examine your username and password!");
			return;
		}
		if (!manager.loginDB()) {
			System.out.println("Login database failed, please re-examine your username and password!");
			return;
		}
		System.out.println("Login succeed!");
		try {
			manager.run();
		} finally {
			manager.close();
		}
	}
}
