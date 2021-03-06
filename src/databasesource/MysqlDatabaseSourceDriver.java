/**
 * Project Name:WebScan
 * File Name:MysqlDatabaseSourceDriver.java
 * Package Name:databasesource
 * Date:2014��1��11������2:50:39
 * Copyright (c) 2014, hzycaicai@163.com All Rights Reserved.
 *
*/

package databasesource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import dataobject.ScanPlan;

/**
 * ClassName:MysqlDatabaseSourceDriver
 *
 * @author   hzycaicai
 * @version  	 
 */
public class MysqlDatabaseSourceDriver extends DatabaseSourceDriver{
	private static Logger logger = Logger.getLogger(SimpleDatabaseSourceDriver.class.getName());
    protected String prefetchConnectionURL;
    protected String writeConnectionURL;
    protected String userName;
    protected String passWd;
    
    protected ArrayList<ScanPlan> scanPlanList = null;
    
    public MysqlDatabaseSourceDriver(String prefetchURL, String writeURL, String userName, String passWd){
    	this.prefetchConnectionURL = prefetchURL;
    	this.writeConnectionURL = writeURL;
    	this.userName = userName;
    	this.passWd = passWd;
    }
    
    /*
     * prefetch the scanPlanList, the list of scanPlans to be scanned
     * mark the status of the items in the list to 1 in database
     */
    protected void startPrefetch(Connection readConn) throws SQLException{
        scanPlanList = new ArrayList<ScanPlan>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = df.format(new Date());
        PreparedStatement pstmt = readConn.prepareStatement("select fid, faddress, ftype, fdatetime, fend, fendibm, Sys_id from TB_SCANPLAN where fdatetime <= ? and fendibm = 0 order by fdatetime");
        //PreparedStatement uptmt;
        pstmt.setString(1, currentTime);
        ResultSet rs = pstmt.executeQuery();
        while(rs.next()){
        	ScanPlan plan = new ScanPlan();
        	int Id = rs.getInt(1);
        	plan.setId(Id);
        	plan.setWebSite(rs.getString(2));
        	plan.setType(rs.getInt(3));
        	plan.setDateTime(rs.getDate(4));
        	plan.setEnd(rs.getInt(5));
        	plan.setEndIbm(rs.getInt(6));
        	plan.setSysId(rs.getInt(7));
        	scanPlanList.add(plan);
        }
        rs.close();
        pstmt.close();
        pstmt = readConn.prepareStatement("update TB_SCANPLAN set fendibm = 1 where fid = ?");
        for(Iterator<ScanPlan> it = scanPlanList.iterator(); it.hasNext();){
        	int Id = it.next().getId();
        	pstmt.setInt(1, Id);
        	pstmt.executeUpdate();
        }
        pstmt.close();
    }
    
    /*
     * @see databasesource.DatabaseSourceDriver#updateScanPlan(int, int)
     * update the item status according the id
     */
    public void updateScanPlan(int fid, int status){
    	try{
    		if(prefetchConnectionURL != null&&userName!=null&&passWd!=null){
	    		Connection conn = DriverManager.getConnection(prefetchConnectionURL,userName,passWd);
	    		PreparedStatement pstmt = conn.prepareStatement("update TB_SCANPLAN set fendibm = ? where fid = ?");
	    		pstmt.setInt(1, status);
	            pstmt.setInt(2, fid);
	            pstmt.executeUpdate();
	            pstmt.close();
    		}
    	} catch(SQLException ex){
    		logger.log(Level.SEVERE, "update db:", ex);
    	}
    }
    
    
	@Override
	public DatabaseSource getNewDatabaseSource() {
		try{
			if(prefetchConnectionURL != null){
				System.out.println(userName+" "+passWd);
				Connection preConn = DriverManager.getConnection(prefetchConnectionURL,userName,passWd);
				logger.info("start db prefetch...");
				startPrefetch(preConn);
				logger.info("prefetch ok~");
			}
			Connection writeConn = DriverManager.getConnection(writeConnectionURL,userName,passWd);
			logger.info("new connection~");
			SimpleDatabaseSource dbs = new SimpleDatabaseSource(writeConn);
			dbs.scanPlanList = this.scanPlanList;
			return dbs;
			
		} catch (SQLException ex) {
            logger.log(Level.SEVERE, "create database connection", ex);
        }
		return null;
	}

	/*
	 * @see databasesource.DatabaseSourceDriver#updateScanPlan(java.util.ArrayList)
	 * update the items in the list with status 0, this is for method client.reset()
	 */
	@Override
	public void updateScanPlan(ArrayList<Integer> list) {
		try{
    		if(prefetchConnectionURL != null&&userName!=null&&passWd!=null&&list!=null&&(list.size()!=0)){
	    		Connection conn = DriverManager.getConnection(prefetchConnectionURL,userName,passWd);
	    		String intList = "(";
	    		for(Iterator<Integer> it = list.iterator();it.hasNext();){
	    			intList = intList + it.next()+",";
	    		}
	    		intList = intList.substring(0,intList.length()-1);
	    		intList+=")";
	    		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	            String currentTime = df.format(new Date());
	    		PreparedStatement pstmt = conn.prepareStatement("update TB_SCANPLAN set fendibm = 0 where fdatetime <= ? and fid not in "+intList);
	    		pstmt.setString(1, currentTime);
		        pstmt.executeUpdate();
	            pstmt.close();
    		}
    	} catch(SQLException ex){
    		logger.log(Level.SEVERE, "update db:", ex);
    	}
	}
}
