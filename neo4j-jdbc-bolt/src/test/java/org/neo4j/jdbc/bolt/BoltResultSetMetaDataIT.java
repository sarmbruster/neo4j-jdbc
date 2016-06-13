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
 * Created on 24/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.jdbc.bolt.data.StatementData;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetMetaDataIT {
	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@Before public void setUp() {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES);
	}

	@After public void tearDown() {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_TWO_PROPERTIES_REV);
	}

	@Test public void getColumnTypeShouldSucceed() throws SQLException, ClassNotFoundException {
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE);
		neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS);

		Connection con = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl());

		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery("MATCH p=((n)-[r]->()) return 'a', 1, 1.0, [1,2,3], {a:1}, null, n, n.name, r, p");
			while (rs.next()) {
				ResultSetMetaData rsm = rs.getMetaData();

				assertEquals(Types.VARCHAR, rsm.getColumnType(1));
				assertEquals("STRING", rsm.getColumnTypeName(1));
				assertEquals(String.class.getName(), rsm.getColumnClassName(1));
				assertTrue((Class.forName(rsm.getColumnClassName(1))).isInstance(rs.getObject(1)));

				assertEquals(Types.INTEGER, rsm.getColumnType(2));
				assertEquals("INTEGER", rsm.getColumnTypeName(2));
				assertEquals(Long.class.getName(), rsm.getColumnClassName(2));
				assertTrue((Class.forName(rsm.getColumnClassName(2))).isInstance(rs.getObject(2)));

				assertEquals(Types.NUMERIC, rsm.getColumnType(3));
				assertEquals("FLOAT", rsm.getColumnTypeName(3));
				assertEquals(Double.class.getName(), rsm.getColumnClassName(3));
				assertTrue((Class.forName(rsm.getColumnClassName(3))).isInstance(rs.getObject(3)));

				assertEquals(Types.ARRAY, rsm.getColumnType(4));
				assertEquals("LIST OF ANY?", rsm.getColumnTypeName(4));
				assertEquals(List.class.getName(), rsm.getColumnClassName(4));
				assertTrue((Class.forName(rsm.getColumnClassName(4))).isInstance(rs.getObject(4)));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(5));
				assertEquals("MAP", rsm.getColumnTypeName(5));
				assertEquals(Map.class.getName(), rsm.getColumnClassName(5));
				assertTrue((Class.forName(rsm.getColumnClassName(5))).isInstance(rs.getObject(5)));

				// If null, it's the default column class, ie string
				assertEquals(Types.VARCHAR, rsm.getColumnType(6));
				assertEquals("STRING", rsm.getColumnTypeName(6));
				assertEquals(String.class.getName(), rsm.getColumnClassName(6));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(7));
				assertEquals("NODE", rsm.getColumnTypeName(7));
				assertEquals(Map.class.getName(), rsm.getColumnClassName(7));
				assertTrue((Class.forName(rsm.getColumnClassName(7))).isInstance(rs.getObject(7)));

				assertEquals(Types.VARCHAR, rsm.getColumnType(8));
				assertEquals("STRING", rsm.getColumnTypeName(8));
				assertEquals(String.class.getName(), rsm.getColumnClassName(8));
				assertTrue((Class.forName(rsm.getColumnClassName(8))).isInstance(rs.getObject(8)));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(9));
				assertEquals("RELATIONSHIP", rsm.getColumnTypeName(9));
				assertEquals(Map.class.getName(), rsm.getColumnClassName(9));
				assertTrue((Class.forName(rsm.getColumnClassName(9))).isInstance(rs.getObject(9)));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(10));
				assertEquals("PATH", rsm.getColumnTypeName(10));
				assertEquals(List.class.getName(), rsm.getColumnClassName(10));
				assertTrue((Class.forName(rsm.getColumnClassName(10))).isInstance(rs.getObject(10)));
			}
		}
	}
}
