package com.gmoutzou.musar;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import prof.onto.Profile;

public class DBConnection {
	
	private final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	
	private String DB_URL;
	private String USER;
	private String PASS;
	
	private Connection conn;
	private PreparedStatement statement;
	private InputStream inputStream;
	private Properties properties;
	
	public DBConnection() {
		conn = null;
		inputStream = null;
		properties = new Properties();
	}
   
	private void loadPropertiesFile() throws NullPointerException, IOException {
		try {
			inputStream = DBConnection.class.getResourceAsStream("/database.properties");
	        properties.load(inputStream);
	        DB_URL = "jdbc:mysql://" 
	        + properties.getProperty("host") 
	        + ":" 
	        + properties.getProperty("port") 
	        + "/"
	        + properties.getProperty("db")
	        + "?useUnicode=yes&characterEncoding=UTF-8";
	        USER = properties.getProperty("user");
	        PASS = properties.getProperty("password");
		} finally {
			inputStream.close();
		}
   }
   
   public boolean createConnection() {
	   boolean success = true;
	   try {
		   loadPropertiesFile();
		   Class.forName(JDBC_DRIVER);
		   System.out.println("Connecting to database...");
		   conn = DriverManager.getConnection(DB_URL, USER, PASS);
	   } catch (Exception e) {
		   e.printStackTrace();
		   success = false;
	   }
	   return success;   
   }
   
   public void closeConnection(){
	   System.out.println("Closing connection to database...");
	   try {
		   if (statement != null) {
			   statement.close();
		   }
		   if (conn != null) {
			   conn.close();
		   }
		   if (inputStream != null) {
			   inputStream.close();
		   }
	   } catch (SQLException se) {
		   se.printStackTrace();
	   } catch(IOException ioe) {
		   ioe.printStackTrace();
	   }
   }
   
   public boolean createProfile(Profile p) {
	   boolean flag = false;
	   if (conn != null) {
		   try {
			   String sql;
			   sql = "INSERT INTO profiles (pr_account, pr_name) values (?, ?)";
			   statement = conn.prepareStatement(sql);
			   statement.setString(1, p.getAccount());
			   statement.setString(2, p.getName());
			   if (statement.executeUpdate() > 0) {
				   flag = true;
			   }
		   } catch (SQLException se) {
			   se.printStackTrace();
		   } catch (Exception e) {
			   e.printStackTrace();
		   } finally {
			   try {
				   if (statement != null) {
					   statement.close();
				   }
			   } catch (SQLException se) {
				   se.printStackTrace();
			   }
		   }
	   }
	   return flag;
   }
   
   public Profile readProfile(String pr_account) {
	   Profile profile = null;
	   if (conn != null) {
		   try {
			   String sql;
			   sql = "SELECT pr_name FROM profiles WHERE pr_account = ?";
			   statement = conn.prepareStatement(sql);
			   statement.setString(1, pr_account);
			   ResultSet rs = statement.executeQuery();
			   if (rs.next()) {
				   profile = new Profile();
				   profile.setAccount(pr_account);
				   profile.setName(rs.getString("pr_name"));
			   }
			   rs.close();
		   } catch (SQLException se) {
			   se.printStackTrace();
		   } catch (Exception e) {
			   e.printStackTrace();
		   } finally {
			   try {
				   if (statement != null) {
					   statement.close();
				   }
			   } catch (SQLException se) {
				   se.printStackTrace();
			   }
		   }
	   }
	   return profile;
   }
   
   public boolean updateProfile(Profile p) {
	   boolean flag = false;
	   if (conn != null) {
		   try {
			   String sql;
			   sql = "UPDATE profiles SET pr_name = ? WHERE pr_account = ?";
			   statement = conn.prepareStatement(sql);
			   statement.setString(1, p.getName());
			   statement.setString(2, p.getAccount());
			   if (statement.executeUpdate() > 0) {
				   flag = true;
			   }
		   } catch (SQLException se) {
			   se.printStackTrace();
		   } catch (Exception e) {
			   e.printStackTrace();
		   } finally {
			   try {
				   if (statement != null) {
					   statement.close();
				   }
			   } catch (SQLException se) {
				   se.printStackTrace();
			   }
		   }
	   }
	   return flag;
   }
   
   public boolean deleteProfile(Profile p) {
	   boolean flag = false;
	   if (conn != null) {
		   try {
			   String sql;
			   sql = "DELETE FROM profiles WHERE pr_account = ?";
			   statement = conn.prepareStatement(sql);
			   statement.setString(1, p.getAccount());
			   if (statement.executeUpdate() > 0) {
				   flag = true;
			   }
		   } catch (SQLException se) {
			   se.printStackTrace();
		   } catch (Exception e) {
			   e.printStackTrace();
		   } finally {
			   try {
				   if (statement != null) {
					   statement.close();
				   }
			   } catch (SQLException se) {
				   se.printStackTrace();
			   }
		   }
	   }
	   return flag;
   }
}