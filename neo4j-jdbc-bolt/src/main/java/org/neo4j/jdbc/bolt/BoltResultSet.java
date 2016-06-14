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
 * Created on 11/02/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.internal.value.*;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.exceptions.value.Uncoercible;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.neo4j.jdbc.*;
import org.neo4j.jdbc.impl.ListArray;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSet extends ResultSet implements Loggable {

	private StatementResult   iterator;
	private ResultSetMetaData metaData;
	private Record            current;
	private List<String>      keys;
	private int               type;
	private int               concurrency;
	private int               holdability;
	private boolean           wasNull;

	public static final int DEFAULT_TYPE        = TYPE_FORWARD_ONLY;
	public static final int DEFAULT_CONCURRENCY = CONCUR_READ_ONLY;
	public static final int DEFAULT_HOLDABILITY = CLOSE_CURSORS_AT_COMMIT;

	private boolean loggable = false;

	/**
	 * Default constructor for this class, if no params are given or if some params are missing it uses the defaults.
	 *
	 * @param statement Statement that produce this ResultSet
	 * @param iterator  The <code>StatementResult</code> of this set
	 * @param params    At most three, type, concurrency and holdability.
	 *                  The defaults are <code>TYPE_FORWARD_ONLY</code>,
	 *                  <code>CONCUR_READ_ONLY</code>,
	 */
	public BoltResultSet(Statement statement, StatementResult iterator, int... params) {
		super(statement);

		this.iterator = iterator;
		this.keys = new ArrayList<>();

		if (this.iterator != null && this.iterator.hasNext() && this.iterator.peek() != null) {
			this.keys = this.iterator.keys();
		}

		this.type = params.length > 0 ? params[0] : TYPE_FORWARD_ONLY;
		this.concurrency = params.length > 1 ? params[1] : CONCUR_READ_ONLY;
		this.holdability = params.length > 2 ? params[2] : CLOSE_CURSORS_AT_COMMIT;

		this.metaData = InstanceFactory.debug(BoltResultSetMetaData.class, new BoltResultSetMetaData(this.iterator, this.keys), this.isLoggable());
	}

	@Override public boolean next() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		if (this.iterator.hasNext()) {
			this.current = this.iterator.next();
		} else {
			this.current = null;
		}
		return this.current != null;
	}

	@Override public void close() throws SQLException {
		if (this.iterator == null) {
			throw new SQLException("ResultCursor not initialized");
		}
		this.isClosed = true;
	}

	@Override public boolean wasNull() throws SQLException {
		checkClosed();
		return this.wasNull;
	}

	@Override public String getString(int columnIndex) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromIndex(columnIndex);
		return this.getStringFromValue(value);
	}

	@Override public String getString(String columnLabel) throws SQLException {
		checkClosed();
		Value value = this.fetchValueFromLabel(columnLabel);
		return this.getStringFromValue(value);
	}

	private String getStringFromValue(Value value) {
		try {
			if (value.isNull()) {
				return null;
			} else {
				return value.asString();
			}
		} catch (Uncoercible e) {
			String result = null;
			if (value instanceof NodeValue) {
				result = this.convertNodeToString(value.asNode());
			} else if (value instanceof RelationshipValue) {
				result = this.convertRelationshipToString(value.asRelationship());
			} else if (value instanceof PathValue) {
				result = this.convertPathToString(value.asPath());
			}
			return result;
		}
	}

	private String convertToJSONProperty(String key, Object value) {
		String result = key == null ? "" : "\"" + key + "\":";

		if (value instanceof String) {
			result += "\"" + value + "\"";
		} else if (value instanceof Number) {
			result += value;
		} else if (value instanceof StringValue) {
			result += "\"" + ((StringValue) value).asString() + "\"";
		} else if (value instanceof IntegerValue) {
			result += ((IntegerValue) value).asInt();
		} else if (value instanceof FloatValue) {
			result += ((FloatValue) value).asFloat();
		} else if (value instanceof BooleanValue) {
			result += ((BooleanValue) value).asBoolean();
		} else if (value instanceof ListValue) {
			result += "[";
			result += this.convertToJSONProperty(null, ((ListValue) value).asList());
			result += "]";
		} else if (value instanceof List) {
			String prefix = "";
			result += "[";
			for (Object obj : (List) value) {
				result += prefix + this.convertToJSONProperty(null, obj);
				prefix = ", ";
			}
			result += "]";
		} else if (value instanceof Iterable) {
			String prefix = "";
			result += "[";
			for (Object obj : (Iterable) value) {
				result += prefix + this.convertToJSONProperty(null, obj);
				prefix = ", ";
			}
			result += "]";
		}

		return result;
	}

	private String convertNodeToString(Node node) {
		String result = "{";

		result += this.convertToJSONProperty("id", node.id()) + ", ";

		result += this.convertToJSONProperty("labels", node.labels()) + (node.size() > 0 ? ", " : "");

		String prefix = "";
		for (String key : node.keys()) {
			result += prefix + this.convertToJSONProperty(key, node.get(key));
			prefix = ", ";
		}

		return result + "}";
	}

	private String convertRelationshipToString(Relationship rel) {
		String result = "{";

		result += this.convertToJSONProperty("id", rel.id()) + ", ";

		result += this.convertToJSONProperty("type", rel.type()) + ", ";

		result += this.convertToJSONProperty("startId", rel.startNodeId()) + ", ";
		result += this.convertToJSONProperty("endId", rel.endNodeId()) + (rel.size() > 0 ? ", " : "");

		String prefix = "";
		for (String key : rel.keys()) {
			result += prefix + this.convertToJSONProperty(key, rel.get(key));
			prefix = ", ";
		}

		return result + "}";
	}

	private String convertPathToString(Path path) {
		String result = "[";

		result += this.convertNodeToString(path.start());

		for (Path.Segment s : path) {
			result += ", " + this.convertRelationshipToString(s.relationship());
			result += ", " + this.convertNodeToString(s.end());
		}

		return result + "]";
	}

	@Override public boolean getBoolean(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asBoolean();
	}

	private Value fetchValueFromLabel(String label) throws SQLException {
		int columnIndex = findColumn(label);
		return fetchValueFromIndex(columnIndex);
	}

	private Value fetchValueFromIndex(int index) throws SQLException {
		Value value;
		if (index > 0 && index - 1 <= this.current.size()) {
			value = this.current.get(index - 1);
		} else {
			//No value found
			throw new SQLException("Column not present in ResultSet");
		}
		this.wasNull = value.isNull();
		return value;
	}

	@Override public int getInt(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asInt();
	}

	@Override public long getLong(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asLong();
	}

	@Override public int findColumn(String columnLabel) throws SQLException {
		checkClosed();
		if (!this.iterator.keys().contains(columnLabel)) {
			throw new SQLException("Column not present in ResultSet");
		}
		return this.iterator.keys().indexOf(columnLabel) + 1;
	}

	@Override public int getType() throws SQLException {
		checkClosed();
		return this.type;
	}

	@Override public int getConcurrency() throws SQLException {
		checkClosed();
		return this.concurrency;
	}

	@Override public int getHoldability() throws SQLException {
		checkClosed();
		return this.holdability;
	}

	@Override public boolean getBoolean(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asBoolean();
	}

	@Override public int getInt(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asInt();
	}

	@Override public long getLong(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asLong();
	}

	@Override public float getFloat(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asFloat();
	}

	@Override public float getFloat(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asFloat();
	}

	@Override public short getShort(String columnLabel) throws SQLException {
		checkClosed();
		return (short) this.fetchValueFromLabel(columnLabel).asInt();
	}

	@Override public short getShort(int columnIndex) throws SQLException {
		checkClosed();
		return (short) this.fetchValueFromIndex(columnIndex).asInt();
	}

	@Override public double getDouble(int columnIndex) throws SQLException {
		checkClosed();
		return this.fetchValueFromIndex(columnIndex).asDouble();
	}

	@Override public Array getArray(int columnIndex) throws SQLException {
		checkClosed();
		Object res = this.fetchValueFromIndex(columnIndex);
		List list = this.objectToList(res);
		return new ListArray(list, Array.getListObjectType(list));
	}

	@Override public Array getArray(String columnLabel) throws SQLException {
		checkClosed();
		Object res = this.fetchValueFromLabel(columnLabel);
		List list = this.objectToList(res);
		return new ListArray(list, Array.getListObjectType(list));
	}

	/**
	 * Convert an Object to a list.
	 *
	 * @param obj The object to convert (must be a list of Node/Relationship or a Path)
	 * @return The corresponding List
	 * @throws SQLException if object cannot be cast to a list
	 */
	private List objectToList(Object obj) throws SQLException {
		List res = null;

		if (ValueAdapter.class.isInstance(obj) && ((ValueAdapter) obj).type() == InternalTypeSystem.TYPE_SYSTEM.LIST()) {
			List list = ((ValueAdapter) obj).asList();

			if (list != null && list.size() > 0) {
				// if it's an object, we convert it as a Map
				if (Array.getObjectType(list.get(0)) == Types.JAVA_OBJECT) {
					List listOfObject = new ArrayList();
					for (Object ob : list) {
						listOfObject.add(generateObject(ob));
					}
					res = listOfObject;
				} else {
					res = list;
				}
			} else {
				res = new ArrayList();
			}
		}
		if (ValueAdapter.class.isInstance(obj) && ((ValueAdapter) obj).type() == InternalTypeSystem.TYPE_SYSTEM.PATH()) {
			res = (List) generateObject(((ValueAdapter) obj).asPath());
		}

		if (res == null) {
			throw new SQLException(obj.getClass().getName() + " can be cast to a list");
		}

		return res;
	}

	@Override public double getDouble(String columnLabel) throws SQLException {
		checkClosed();
		return this.fetchValueFromLabel(columnLabel).asDouble();
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return metaData;
	}

	private Object generateObject(Object obj) {
		if (obj instanceof Node) {
			Node node = (Node) obj;
			Map<String, Object> map = new HashMap<>();
			map.put("_id", node.id());
			map.put("_labels", node.labels());
			map.putAll(node.asMap());
			return map;
		}
		if (obj instanceof Relationship) {
			Relationship rel = (Relationship) obj;
			Map<String, Object> map = new HashMap<>(16);
			map.put("_id", rel.id());
			map.put("_type", rel.type());
			map.put("_startId", rel.startNodeId());
			map.put("_endId", rel.endNodeId());
			map.putAll(rel.asMap());
			return map;
		}
		if (obj instanceof Path) {
			Path path = (Path) obj;
			List<Object> list = new ArrayList<>(path.length());
			list.add(this.generateObject(path.start()));
			for (Path.Segment segment : path) {
				list.add(this.generateObject(segment.relationship()));
				list.add(this.generateObject(segment.end()));
			}
			return list;
		}
		return obj;
	}

	@Override public Object getObject(int columnIndex) throws SQLException {
		checkClosed();
		Object result = null;
		Value value = this.fetchValueFromIndex(columnIndex);

		if (InternalTypeSystem.TYPE_SYSTEM.STRING().equals(value.type())) {
			result = this.getString(columnIndex);
		}
		if (InternalTypeSystem.TYPE_SYSTEM.INTEGER().equals(value.type())) {
			result = this.getLong(columnIndex);
		}
		if (InternalTypeSystem.TYPE_SYSTEM.BOOLEAN().equals(value.type())) {
			result = this.getBoolean(columnIndex);
		}
		if (InternalTypeSystem.TYPE_SYSTEM.FLOAT().equals(value.type())) {
			result = this.getDouble(columnIndex);
		}
		if (InternalTypeSystem.TYPE_SYSTEM.NODE().equals(value.type())) {
			result = this.generateObject(value.asObject());
		}
		if (InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP().equals(value.type())) {
			result = this.generateObject(value.asObject());
		}
		if (InternalTypeSystem.TYPE_SYSTEM.PATH().equals(value.type())) {
			result = this.generateObject(value.asObject());
		}
		if (InternalTypeSystem.TYPE_SYSTEM.MAP().equals(value.type())) {
			result = value.asMap();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.ANY().equals(value.type())) {
			result = value.asObject();
		}
		if (InternalTypeSystem.TYPE_SYSTEM.NULL().equals(value.type())) {
			result = null;
		}
		if (InternalTypeSystem.TYPE_SYSTEM.LIST().equals(value.type())) {
			result = this.objectToList(value);
		}

		return result;
	}

	@Override public Object getObject(String columnLabel) throws SQLException {
		checkClosed();
		int colIndex = this.findColumn(columnLabel);
		return this.getObject(colIndex);
	}

	public StatementResult getIterator() {
		return this.iterator;
	}

	public List<String> getKeys() {
		return this.keys;
	}

	/*--------------------*/
	/*       Logger       */
	/*--------------------*/

	@Override public boolean isLoggable() {
		return this.loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}

}
