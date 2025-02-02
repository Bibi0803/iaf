package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.hamcrest.core.StringStartsWith;
import org.hamcrest.text.IsEmptyString;
import org.junit.Test;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

public class DbmsSupportTest extends JdbcTestBase {
	protected static Logger log = LogUtil.getLogger(DbmsSupportTest.class);

	private boolean testPeekFindsRecordsWhenTheyAreAvailable = true;
	private boolean testSkipLocked;


	public DbmsSupportTest(DataSource dataSource) throws SQLException {
		super(dataSource);
		testSkipLocked = dbmsSupport.hasSkipLockedFunctionality();
	}

	@Test
	public void testGetDbmsSupport() {
		assertNotNull(dbmsSupport);
	}

	@Test
	public void testName() {
		assertEquals(productKey, dbmsSupport.getDbmsName());
		assertEquals(productKey, dbmsSupport.getDbms().getKey());
	}

	@Test
	public void testIsTablePresent() throws JdbcException {
		assertTrue("Should have found existing table", dbmsSupport.isTablePresent(connection, "TEMP"));
		assertFalse(dbmsSupport.isTablePresent(connection, "XXXX"));
	}

	@Test
	public void testIsColumnPresent() throws JdbcException {
		assertTrue("Should have found existing column", dbmsSupport.isColumnPresent(connection, "TEMP", "TINT"));
		assertFalse(dbmsSupport.isColumnPresent(connection, "TEMP", "XXXX"));
		assertFalse(dbmsSupport.isColumnPresent(connection, "XXXX", "XXXX"));
	}

	@Test
	public void testIsColumnPresentInSchema() throws JdbcException {
		String schema = dbmsSupport.getSchema(connection);
		assertTrue("Should have found existing column in schema ["+schema+"]", dbmsSupport.isColumnPresent(connection, schema, "TEMP", "TINT"));
		assertFalse(dbmsSupport.isColumnPresent(connection, schema, "TEMP", "XXXX"));
		assertFalse(dbmsSupport.isColumnPresent(connection, schema, "XXXX", "XXXX"));
	}

	@Test
	public void testGetDateTimeLiteral() throws Exception {
		JdbcUtil.executeStatement(connection, "INSERT INTO TEMP(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (1,2,3,"+dbmsSupport.getDateAndOffset(dbmsSupport.getDatetimeLiteral(new Date()),4)+","+dbmsSupport.getDatetimeLiteral(new Date())+")");
		Object result = JdbcUtil.executeQuery(dbmsSupport, connection, "SELECT "+dbmsSupport.getTimestampAsDate("TDATETIME")+" FROM TEMP WHERE TKEY=1", null);
		System.out.println("result:"+result);
	}

	@Test
	public void testSysDate() throws Exception {
		JdbcUtil.executeStatement(connection, "INSERT INTO TEMP(TKEY, TVARCHAR, TINT, TDATE, TDATETIME) VALUES (2,'xxx',3,"+dbmsSupport.getSysDate()+","+dbmsSupport.getSysDate()+")");
		Object result = JdbcUtil.executeQuery(dbmsSupport, connection, "SELECT "+dbmsSupport.getTimestampAsDate("TDATETIME")+" FROM TEMP WHERE TKEY=2", null);
		System.out.println("result:"+result);
	}
	
	@Test
	public void testNumericAsDouble() throws Exception {
		String number = "1234.5678";
		QueryExecutionContext context = new QueryExecutionContext("INSERT INTO TEMP(TKEY, TNUMBER) VALUES (3,?)", QueryType.OTHER, null);
		dbmsSupport.convertQuery(context, "Oracle");
		System.out.println("executing query ["+context.getQuery()+"]");
		try (PreparedStatement stmt = connection.prepareStatement(context.getQuery())) {
			stmt.setDouble(1, Double.parseDouble(number));
			stmt.execute();
		}
		
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER FROM TEMP WHERE TKEY=3", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
			}
		}
	}
	@Test
	public void testNumericAsFloat() throws Exception {
		assumeFalse(dbmsSupport.getDbms()==Dbms.POSTGRESQL); // This fails on PostgreSQL, precision of setFloat appears to be too low"
		String number = "1234.5677";
		QueryExecutionContext context = new QueryExecutionContext("INSERT INTO TEMP(TKEY, TNUMBER) VALUES (4,?)", QueryType.OTHER, null);
		dbmsSupport.convertQuery(context, "Oracle");
		System.out.println("executing query ["+context.getQuery()+"]");
		try (PreparedStatement stmt = connection.prepareStatement(context.getQuery())) {
			stmt.setFloat(1, Float.parseFloat(number));
			stmt.execute();
		}
		
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER FROM TEMP WHERE TKEY=4", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
			}
		}
	}

	@Test 
	// test the alias functionality as used in JdbcTableListener. 
	// Asserts that columns can be identified with and without alias.
	public void testSelectWithAlias() throws Exception {
		String insertQuery = "INSERT INTO TEMP(TKEY, TNUMBER, TVARCHAR) VALUES (5,5,'A')";
		String selectQuery = "SELECT TNUMBER FROM TEMP t WHERE TKEY=5 AND t.TVARCHAR='A'";
		System.out.println("executing query ["+insertQuery+"]");
		try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
			stmt.execute();
		}
		
		try (PreparedStatement stmt = executeTranslatedQuery(connection, selectQuery, QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertEquals(5,resultSet.getInt(1));
			}
		}
	}
	
	@Test
	public void testJdbcSetParameter() throws Exception {
		String number = "1234.5678";
		String datetime = DateUtils.format(new Date(), DateUtils.FORMAT_GENERICDATETIME);
		String date = DateUtils.format(new Date(), DateUtils.shortIsoFormat);
		
		assumeFalse(dbmsSupport.getDbmsName().equals("Oracle")); // This fails on Oracle, cannot set a non-integer number via setString()
		QueryExecutionContext context = new QueryExecutionContext("INSERT INTO TEMP(TKEY, TNUMBER, TDATE, TDATETIME) VALUES (5,?,?,?)", QueryType.OTHER, null);
		dbmsSupport.convertQuery(context, "Oracle");
		System.out.println("executing query ["+context.getQuery()+"]");
		try (PreparedStatement stmt = connection.prepareStatement(context.getQuery())) {
			JdbcUtil.setParameter(stmt, 1, number, dbmsSupport.isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 2, date, dbmsSupport.isParameterTypeMatchRequired());
			JdbcUtil.setParameter(stmt, 3, datetime, dbmsSupport.isParameterTypeMatchRequired());
			//JdbcUtil.setParameter(stmt, 4, bool, dbmsSupport.isParameterTypeMatchRequired());
			stmt.execute();
		}
		
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TNUMBER, TDATE, TDATETIME FROM TEMP WHERE TKEY=5", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(resultSet.getString(1), StringStartsWith.startsWith(number));
				assertEquals(date, resultSet.getString(2));
				assertThat(resultSet.getString(3), StringStartsWith.startsWith(datetime));
				//assertEquals(Boolean.parseBoolean(bool), resultSet.getBoolean(4));
			}
		}
	}

	
	@Test
	public void testWriteAndReadClob() throws Exception {
		String clobContents = "Dit is de content van de clob";
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TCLOB) VALUES (10,EMPTY_CLOB())", QueryType.OTHER);
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM TEMP WHERE TKEY=10 FOR UPDATE", QueryType.SELECT, true)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Object clobHandle = dbmsSupport.getClobHandle(resultSet, 1);
				try (Writer writer = dbmsSupport.getClobWriter(resultSet, 1, clobHandle)) {
					writer.append(clobContents);
				}
				dbmsSupport.updateClob(resultSet, 1, clobHandle);
				resultSet.updateRow();
			}
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM TEMP WHERE TKEY=10", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals(clobContents, actual);
			}
		}
		
	}

	@Test
	public void testReadEmptyClob() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TCLOB) VALUES (11,EMPTY_CLOB())", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM TEMP WHERE TKEY=11", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals("", actual);
			}
		}
	}

	@Test
	public void testReadNullClob() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY) VALUES (11)", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM TEMP WHERE TKEY=11", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertNull(dbmsSupport.getClobReader(resultSet, 1));
				assertTrue(resultSet.wasNull());
			}
		}
	}


	@Test
	public void testWriteClobInOneStep() throws Exception {
		String clobContents = "Dit is de content van de clob";
		QueryExecutionContext context = new QueryExecutionContext("INSERT INTO TEMP (TKEY,TCLOB) VALUES (12,?)", QueryType.OTHER, null);
		dbmsSupport.convertQuery(context, "Oracle");
		try (PreparedStatement stmt = connection.prepareStatement(context.getQuery());) {
			stmt.setString(1, clobContents);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM TEMP WHERE TKEY=12", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Reader clobReader = dbmsSupport.getClobReader(resultSet, 1);
				String actual = StreamUtil.readerToString(clobReader, null);
				assertEquals(clobContents, actual);
			}
		}
		
	}

	@Test
	public void testInsertEmptyClobUsingDbmsSupport() throws Exception {
		
		JdbcUtil.executeStatement(connection, "INSERT INTO TEMP (TKEY,TCLOB) VALUES (13,"+dbmsSupport.emptyClobValue()+")");

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TCLOB FROM TEMP WHERE TKEY=13", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(JdbcUtil.getClobAsString(dbmsSupport, resultSet, 1, false), IsEmptyString.isEmptyOrNullString() );
			}
		}
	}


	@Test
	public void testWriteAndReadBlob() throws Exception {
		String blobContents = "Dit is de content van de blob";
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TBLOB) VALUES (20,EMPTY_BLOB())", QueryType.OTHER);
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM TEMP WHERE TKEY=20 FOR UPDATE", QueryType.SELECT, true)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Object blobHandle = dbmsSupport.getBlobHandle(resultSet, 1);
				try (OutputStream out = dbmsSupport.getBlobOutputStream(resultSet, 1, blobHandle)) {
					out.write(blobContents.getBytes("UTF-8"));
				}
				dbmsSupport.updateBlob(resultSet, 1, blobHandle);
				resultSet.updateRow();
			}
		}		
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM TEMP WHERE TKEY=20", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream blobStream = dbmsSupport.getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(blobStream, null, "UTF-8");
				assertEquals(blobContents, actual);
			}
		}
		
	}
	

	@Test
	public void testWriteAndReadBlobCompressed() throws Exception {
		String blobContents = "Dit is de content van de blob";
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TBLOB) VALUES (21,EMPTY_BLOB())", QueryType.OTHER);
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM TEMP WHERE TKEY=21 FOR UPDATE", QueryType.SELECT, true)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				Object blobHandle = dbmsSupport.getBlobHandle(resultSet, 1);

				try (OutputStream blobOutputStream = JdbcUtil.getBlobOutputStream(dbmsSupport, blobHandle, resultSet, 1, true)) {
					blobOutputStream.write(blobContents.getBytes("UTF-8"));
				}
				dbmsSupport.updateBlob(resultSet, 1, blobHandle);
				resultSet.updateRow();
			}
		}		
		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM TEMP WHERE TKEY=21", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				String actual = JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", true, false, false);
				assertEquals(blobContents, actual);
			}
		}
		
	}

	@Test
	public void testReadEmptyBlob() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TBLOB) VALUES (22,EMPTY_BLOB())", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM TEMP WHERE TKEY=22", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				InputStream inputStream = dbmsSupport.getBlobInputStream(resultSet, 1);
				String actual = StreamUtil.streamToString(inputStream, null, null);
				assertEquals("", actual);
			}
		}
	}

	@Test
	public void testReadNullBlob() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY) VALUES (23)", QueryType.OTHER);

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM TEMP WHERE TKEY=23", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertNull(dbmsSupport.getClobReader(resultSet, 1));
				assertTrue(resultSet.wasNull());
			}
		}
	}

	@Test
	public void testWriteBlobInOneStep() throws Exception {
		String blobContents = "Dit is de content van de blob";
		QueryExecutionContext context = new QueryExecutionContext("INSERT INTO TEMP (TKEY,TBLOB) VALUES (24,?)", QueryType.OTHER, null);
		dbmsSupport.convertQuery(context, "Oracle");
		try (PreparedStatement stmt = connection.prepareStatement(context.getQuery());) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM TEMP WHERE TKEY=24", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				String actual = JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", false, false, false);
				assertEquals(blobContents, actual);
			}
		}
		
	}

	@Test
	public void testInsertEmptyBlobUsingDbmsSupport() throws Exception {
		
		JdbcUtil.executeStatement(connection, "INSERT INTO TEMP (TKEY,TBLOB) VALUES (25,"+dbmsSupport.emptyBlobValue()+")");

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB FROM TEMP WHERE TKEY=25", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				resultSet.next();
				assertThat(JdbcUtil.getBlobAsString(dbmsSupport, resultSet, 1, "UTF-8", false, false, false), IsEmptyString.isEmptyOrNullString() );
			}
		}
		
	}

	@Test
	public void testReadBlobAndCLobUsingJdbcUtilGetValue() throws Exception {
		String blobContents = "Dit is de content van de blob";
		String clobContents = "Dit is de content van de clob";
		QueryExecutionContext context = new QueryExecutionContext("INSERT INTO TEMP (TKEY,TBLOB,TCLOB) VALUES (24,?,?)", QueryType.OTHER, null);
		dbmsSupport.convertQuery(context, "Oracle");
		try (PreparedStatement stmt = connection.prepareStatement(context.getQuery());) {
			stmt.setBytes(1, blobContents.getBytes("UTF-8"));
			stmt.setString(2, clobContents);
			stmt.execute();
		}

		try (PreparedStatement stmt = executeTranslatedQuery(connection, "SELECT TBLOB,TCLOB FROM TEMP WHERE TKEY=24", QueryType.SELECT)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				ResultSetMetaData rsmeta = resultSet.getMetaData();
				resultSet.next();
				String actual1 = JdbcUtil.getValue(dbmsSupport, resultSet, 1, rsmeta, "UTF-8", false, null, true, false, false);
				String actual2 = JdbcUtil.getValue(dbmsSupport, resultSet, 2, rsmeta, "UTF-8", false, null, true, false, false);
				assertEquals(blobContents, actual1);
				assertEquals(clobContents, actual2);
			}
		}
		
	}


	
	@Test
	public void testBooleanHandling() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TINT,TBOOLEAN) VALUES (30,99,"+dbmsSupport.getBooleanValue(false)+")", QueryType.OTHER);
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TINT,TBOOLEAN) VALUES (31,99,"+dbmsSupport.getBooleanValue(true)+")", QueryType.OTHER);
		
		assertEquals(30, JdbcUtil.executeIntQuery(connection, "SELECT TKEY FROM TEMP WHERE TINT=99 AND TBOOLEAN="+dbmsSupport.getBooleanValue(false)));
		assertEquals(31, JdbcUtil.executeIntQuery(connection, "SELECT TKEY FROM TEMP WHERE TINT=99 AND TBOOLEAN="+dbmsSupport.getBooleanValue(true)));
		
	}
	
	private boolean peek(String query) throws Exception {
		try (Connection peekConnection=getConnection()) {
			peekConnection.setAutoCommit(false);
			return !JdbcUtil.isQueryResultEmpty(peekConnection, query);
		}
	}
	
	@Test
	public void testQueueHandling() throws Exception {
		executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (40,100)", QueryType.OTHER);

		String selectQuery="SELECT TKEY FROM TEMP WHERE TINT=100";
		assertEquals(40, JdbcUtil.executeIntQuery(connection, selectQuery));

		String readQueueQuery = dbmsSupport.prepareQueryTextForWorkQueueReading(1, selectQuery);
		String peekQueueQuery = dbmsSupport.prepareQueryTextForWorkQueuePeeking(1, selectQuery);
		
		// test that peek and read find records when they are available
		assertEquals(40, JdbcUtil.executeIntQuery(connection, peekQueueQuery));
		assertEquals(40, JdbcUtil.executeIntQuery(connection, readQueueQuery));
		assertEquals(40, JdbcUtil.executeIntQuery(connection, peekQueueQuery));
		
		try (Connection workConn1=getConnection()) {
			workConn1.setAutoCommit(false);
			try (Statement stmt1= workConn1.createStatement()) {
				stmt1.setFetchSize(1);
				log.debug("Read queue using query ["+readQueueQuery+"]");
				try (ResultSet rs1=stmt1.executeQuery(readQueueQuery)) {
					assertTrue(rs1.next());
					assertEquals(40,rs1.getInt(1));			// find the first record
					if (testPeekShouldSkipRecordsAlreadyLocked) assertFalse("Peek should skip records already locked, but it found one", peek(peekQueueQuery));	// assert no more records found

					if (testSkipLocked) {
						try (Connection workConn2=getConnection()) {
							workConn2.setAutoCommit(false);
							try (Statement stmt2= workConn2.createStatement()) {
								stmt2.setFetchSize(1);
								try (ResultSet rs2=stmt2.executeQuery(readQueueQuery)) {
									if (rs2.next()) { // shouldn't find record in QueueReading mode either
										fail("readQueueQuery ["+readQueueQuery+"] should not have found record ["+rs2.getString(1)+"] that is already locked");
									}
								}
							}
							workConn2.commit();
						}

						// insert another record
						executeTranslatedQuery(connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (41,100)", QueryType.OTHER);
						if (testPeekFindsRecordsWhenTheyAreAvailable) assertTrue("second record should have been seen by peek query", peek(peekQueueQuery));// assert that record is seen
						
						try (Connection workConn2=getConnection()) {
							workConn2.setAutoCommit(false);
							try (Statement stmt2= workConn2.createStatement()) {
								stmt2.setFetchSize(1);
								try (ResultSet rs2=stmt2.executeQuery(readQueueQuery)) {
									assertTrue(rs2.next());
									assertEquals(41,rs2.getInt(1));	// find the second record
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Test
	public void testIsBlobType() throws SQLException {
		try (Connection connection=getConnection()) {
			try (PreparedStatement stmt= connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM TEMP")) {
				try (ResultSet rs=stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i=1;i<=9;i++) {
						assertEquals("column type name ["+rsmeta.getColumnTypeName(i)+"] precision ["+rsmeta.getPrecision(i)+"] column type ["+rsmeta.getColumnType(i)+"]", i==8, dbmsSupport.isBlobType(rsmeta, i));
					}
				}
				
			}
		}
	}
	
	
	
	@Test
	public void testIsClobType() throws SQLException {
		try (Connection connection=getConnection()) {
			try (PreparedStatement stmt= connection.prepareStatement("SELECT TKEY, TINT, TVARCHAR, TNUMBER, TDATE, TDATETIME, TBOOLEAN, TBLOB, TCLOB FROM TEMP")) {
				try (ResultSet rs=stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i=1;i<=9;i++) {
						assertEquals("column type name ["+rsmeta.getColumnTypeName(i)+"] precision ["+rsmeta.getPrecision(i)+"] column type ["+rsmeta.getColumnType(i)+"]", i==9, dbmsSupport.isClobType(rsmeta, i));
					}
				}
				
			}
		}
	}

	@Test
	public void testIsBlobTypeIbisTemp() throws Exception {
		try (Connection connection=getConnection()) {
			assumeTrue(dbmsSupport.isTablePresent(connection, "IBISTEMP"));
			try (PreparedStatement stmt= connection.prepareStatement("SELECT TKEY, TVARCHAR, TNUMBER, TDATE, TTIMESTAMP, TBLOB1, TCLOB FROM IBISTEMP")) {
				try (ResultSet rs=stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i=1;i<=7;i++) {
						assertEquals("column type name ["+rsmeta.getColumnTypeName(i)+"] precision ["+rsmeta.getPrecision(i)+"] column type ["+rsmeta.getColumnType(i)+"]", i==6, dbmsSupport.isBlobType(rsmeta, i));
					}
				}
				
			}
		}
	}
	
	@Test
	public void testIsClobTypeIbisTemp() throws Exception {
		try (Connection connection=getConnection()) {
			assumeTrue(dbmsSupport.isTablePresent(connection, "IBISTEMP"));
			try (PreparedStatement stmt= connection.prepareStatement("SELECT TKEY, TVARCHAR, TNUMBER, TDATE, TTIMESTAMP, TBLOB1, TCLOB FROM IBISTEMP")) {
				try (ResultSet rs=stmt.executeQuery()) {
					ResultSetMetaData rsmeta = rs.getMetaData();
					for (int i=1;i<=7;i++) {
						assertEquals("column type name ["+rsmeta.getColumnTypeName(i)+"] precision ["+rsmeta.getPrecision(i)+"] column type ["+rsmeta.getColumnType(i)+"]", i==7, dbmsSupport.isClobType(rsmeta, i));
					}
				}
				
			}
		}
	}

}
