/*
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 08/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.Result;
import org.neo4j.jdbc.bolt.data.StatementData;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltStatementIT {

	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/
	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL_STRING);

		assertTrue(rs.next());
		assertEquals("test", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
	}

	@Test public void executeQueryWithObjectShouldExecuteAndReturnAMap() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_NODES_MORE);

		assertTrue(rs.next());

		// Testing Meta for column type
		assertEquals("java.util.Map", rs.getMetaData().getColumnClassName(1));
		assertEquals("java.util.Map", rs.getMetaData().getColumnClassName(2));
		assertEquals("java.util.Map", rs.getMetaData().getColumnClassName(3));

		// Testing Value
		assertEquals("test", rs.getObject(1, HashMap.class).get("name").toString());
		assertEquals(Long.valueOf("1459248821051"), (Long) rs.getObject(2, HashMap.class).get("date"));
		assertEquals(Boolean.TRUE, (Boolean) rs.getObject(3, HashMap.class).get("status"));

		assertFalse(rs.next());
		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
	}

	@Test public void executeQueryWithArrayShouldExecuteAndReturnArray() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_COLLECT_USERNAME);

		assertTrue(rs.next());

		// Testing Meta for column type
		assertEquals("java.util.List", rs.getMetaData().getColumnClassName(1));
		List<Map> list = (List<Map>) rs.getObject(1, List.class);
		assertEquals("test", list.get(0));

		// Testing Value with array
		java.sql.Array sqlArray = rs.getArray(1);
		String[] javaArray = (String[]) sqlArray.getArray();
		assertEquals("test", javaArray[0]);

		assertFalse(rs.next());
		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
	}

	@Test public void executeQueryWithArrayOfNodeShouldExecuteAndReturnArrayOfMap() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_COLLECT_USER);

		assertTrue(rs.next());

		// Testing Value with object
		assertEquals("java.util.List", rs.getMetaData().getColumnClassName(1));
		List<Map> list = rs.getObject(1, List.class);
		assertEquals("test", list.get(0).get("name").toString());
		assertEquals("testAgain", list.get(0).get("surname").toString());

		// Testing value with Array
		java.sql.Array sqlArray = rs.getArray(1);
		Map[] javaArray = (Map[]) sqlArray.getArray();
		assertEquals("test", javaArray[0].get("name").toString());
		assertEquals("testAgain", javaArray[0].get("surname").toString());

		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
	}

	@Test public void executeQueryWithArrayOfRelationshipShouldExecuteAndReturnArrayOfMap() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_COLLECT_REL);

		assertTrue(rs.next());

		// Testing value with list
		assertEquals("java.util.List", rs.getMetaData().getColumnClassName(1));
		List<Map> list = (List<Map>) rs.getObject(1);
		assertEquals(Long.valueOf("1459248821051"), (Long) list.get(0).get("date"));

		// Testing value with Array
		java.sql.Array sqlArray = rs.getArray(1);
		Map[] javaArray = (Map[]) sqlArray.getArray();
		assertEquals(Long.valueOf("1459248821051"), (Long) javaArray[0].get("date"));

		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
	}

	@Test public void executeQueryWithEmptyArrayShouldExecuteAndReturnEmptyArray() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_COLLECT_USER);

		assertTrue(rs.next());

		// Testing Meta for column type
		assertEquals("java.util.List", rs.getMetaData().getColumnClassName(1));
		List<Map> list = (List<Map>) rs.getObject(1);
		assertEquals(0, list.size());

		// Testing Value
		java.sql.Array sqlArray = rs.getArray(1);
		Object[] javaArray = (Object[]) sqlArray.getArray();
		assertEquals(0, javaArray.length);

		connection.close();
	}

	@Test public void executeQueryWithPathShouldExecuteAndReturnAList() throws SQLException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_PATH);

		assertTrue(rs.next());

		// Testing Meta for column type
		assertEquals("java.util.List", rs.getMetaData().getColumnClassName(1));

		// Testing Value
		// ~~~~~~~~~~~~~
		java.sql.Array sqlArray = rs.getArray(1);
		Map[] javaArray = (Map[]) sqlArray.getArray();

		// Node
		assertEquals("test", javaArray[0].get("name").toString());
		assertEquals("testAgain", javaArray[0].get("surname").toString());

		// Rel
		assertEquals(Long.valueOf("1459248821051"), (Long) javaArray[1].get("date"));

		// Node
		assertEquals(Boolean.TRUE, (Boolean) javaArray[2].get("status"));

		connection.close();
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
	}

	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/
	@Test public void executeUpdateShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		int lines = statement.executeUpdate(StatementData.STATEMENT_CREATE);
		assertEquals(1, lines);

		lines = statement.executeUpdate(StatementData.STATEMENT_CREATE);
		assertEquals(1, lines);

		lines = statement.executeUpdate(StatementData.STATEMENT_CLEAR_DB);
		assertEquals(2, lines);

		connection.close();
	}

	/*------------------------------*/
	/*            execute           */
	/*------------------------------*/
	@Test public void executeShouldExecuteAndReturnFalse() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		boolean result = statement.execute(StatementData.STATEMENT_CREATE);
		assertFalse(result);

		result = statement.execute(StatementData.STATEMENT_CLEAR_DB);
		assertFalse(result);

		connection.close();
	}

	@Test public void executeShouldExecuteAndReturnTrue() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		boolean result = statement.execute(StatementData.STATEMENT_MATCH_ALL);
		assertTrue(result);

		connection.close();
	}

	@Test public void executeBadCypherQueryOnAutoCommitShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid input");

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());

		Statement statement = connection.createStatement();
		try {
			statement.execute("AZERTYUIOP");
		} finally {
			connection.close();
		}
	}

	@Test public void executeBadCypherQueryWithoutAutoCommitShouldReturnAnSQLException() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid input");

		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		connection.setAutoCommit(false);

		Statement statement = connection.createStatement();
		try {
			statement.execute("AZERTYUIOP");
		} finally {
			connection.close();
		}
	}

	/*------------------------------*/
	/*         executeBatch         */
	/*------------------------------*/
	@Test public void executeBatchShouldWork() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		connection.setAutoCommit(true);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);

		int[] result = statement.executeBatch();

		assertArrayEquals(new int[] { 1, 1, 1 }, result);

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);

		connection.close();
	}

	@Test public void executeBatchShouldWorkWhenError() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		connection.setAutoCommit(true);
		Statement statement = connection.createStatement();
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch("wrong query");
		statement.addBatch(StatementData.STATEMENT_CREATE);

		try {
			statement.executeBatch();
			fail();
		} catch (BatchUpdateException e) {
			assertArrayEquals(new int[] { 1, 1 }, e.getUpdateCounts());
		}

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
		connection.close();
	}

	@Test public void executeBatchShouldWorkWithTransaction() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());
		Statement statement = connection.createStatement();
		connection.setAutoCommit(false);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);
		statement.addBatch(StatementData.STATEMENT_CREATE);

		Result res = neo4j.getGraphDatabase().execute(StatementData.STATEMENT_COUNT_NODES);
		while (res.hasNext()) {
			assertEquals(0L, res.next().get("total"));
		}

		int[] result = statement.executeBatch();

		assertArrayEquals(new int[] { 1, 1, 1 }, result);

		connection.commit();

		res = neo4j.getGraphDatabase().execute(StatementData.STATEMENT_COUNT_NODES);
		while (res.hasNext()) {
			assertEquals(3L, res.next().get("total"));
		}

		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);

		connection.close();
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/
	@Test public void closeShouldNotCloseTransaction() throws SQLException {
		try (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl())) {
			connection.setAutoCommit(false);

			Statement statement = connection.createStatement();
			statement.execute("RETURN true AS result");
			statement.close();

			assertTrue(((BoltConnection) connection).getTransaction().isOpen());
		}
	}

}
