package com.github.beast.database;

import java.util.ArrayList;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;

import com.github.beast.Beast;

/**
 * Graph database.
 * 
 * @author Štefan Sabo
 * @version 1.0
 */
public class GraphDatabase {

	/** Graph database object. */
	private GraphDatabaseService graphDb;

	/**
	 * Constructs a new <code>GraphDatabase</code>, using the given
	 * <code>path</code> as a directory for local storage.
	 * 
	 * @param path the directory, where database will be saved
	 */
	public GraphDatabase(final String path) {

		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(path);
		registerShutdownHook(graphDb);
	}

	/**
	 * Create a new index of the given name.
	 * 
	 * @param name the name of index to be created
	 * @return created index
	 */
	protected Index<Node> createIndex(final String name) {

		return graphDb.index().forNodes(name);
	}

	/**
	 * Adds hook to ensure correct shutdown of database in case of unexpected
	 * exit.
	 * 
	 * @param graphDb graph database to add shutdown hook to
	 */
	private static void registerShutdownHook(final GraphDatabaseService graphDb) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {

				graphDb.shutdown();
			}
		});
	}

	/**
	 * Shutdown the running database.
	 */
	public void shutdown() {

		Beast.log("shutting down");
		graphDb.shutdown();
	}

	/**
	 * Create a new node. Wraps
	 * {@link org.neo4j.graphdb.GraphDatabaseService#createNode() createNode()}
	 * method of database and executes it in transaction.
	 * 
	 * @return created node
	 * @see #createNode(Map)
	 */
	protected Node createNode() {

		Transaction tx = graphDb.beginTx();
		Node node;

		try {
			node = graphDb.createNode();
			tx.success();
		} finally {
			tx.finish();
		}
		return node;
	}

	/**
	 * Create a new node and set a single property, according to supplied
	 * values.
	 * 
	 * @param key the key of property to be set
	 * @param value the value of property to be set
	 * @return created node
	 */
	protected Node createNode(final String key, final Object value) {

		Node node = createNode();
		Transaction tx = graphDb.beginTx();

		try {
			node.setProperty(key, value);
			tx.success();
		} finally {
			tx.finish();
		}
		return node;
	}

	/**
	 * Create a new node and set its properties, according to supplied values.
	 * Wraps {@link org.neo4j.graphdb.GraphDatabaseService#createNode()
	 * createNode()} method of database and executes it in transaction.
	 * 
	 * @param properties map of property - value pairs to be set for created
	 *        node
	 * @return created node
	 */
	protected Node createNode(final Map<String, Object> properties) {

		Node node = createNode();
		Transaction tx = graphDb.beginTx();

		try {
			for (Map.Entry<String, Object> property : properties.entrySet()) {
				node.setProperty(property.getKey(), property.getValue());
			}
			tx.success();
		} finally {
			tx.finish();
		}
		return node;
	}

	/**
	 * Sets a property of a given node. Wraps
	 * {@link org.neo4j.graphdb.PropertyContainer#setProperty(String, Object)
	 * setProperty(String, Object)} method of database and executes it in
	 * transaction. If the node already has a property with the given key, the
	 * property is overwritten.
	 * 
	 * @param node the node to have its property set
	 * @param key the key of property to be set
	 * @param value the value of property to be set
	 */
	protected void setProperty(final Node node, final String key, final Object value) {

		Transaction tx = graphDb.beginTx();

		try {
			node.setProperty(key, value);
			tx.success();
		} finally {
			tx.finish();
		}
	}

	/**
	 * Sets a property of a given relationship. Wraps
	 * {@link org.neo4j.graphdb.PropertyContainer#setProperty(String, Object)
	 * setProperty(String, Object)} method of database and executes it in
	 * transaction. If the relationship already has a property with the given
	 * key, the property is overwritten.
	 * 
	 * @param relationship the relationship to have its property set
	 * @param key the key of property to be set
	 * @param value the value of property to be set
	 */
	protected void setProperty(final Relationship relationship, final String key, final Object value) {

		Transaction tx = graphDb.beginTx();

		try {
			relationship.setProperty(key, value);
			tx.success();
		} finally {
			tx.finish();
		}
	}

	/**
	 * Creates a new relationship between two given nodes. Wraps
	 * {@link org.neo4j.graphdb.Node#createRelationshipTo(Node, RelationshipType)
	 * createRelationshipType} of database and executes it in transaction.
	 * 
	 * @param firstNode starting node
	 * @param secondNode ending node
	 * @param relationshipType type of the relationship to be created
	 * @return created relationship
	 */
	protected Relationship addRelationship(final Node firstNode, final Node secondNode,
			final RelationshipType relationshipType) {

		Transaction tx = graphDb.beginTx();
		Relationship rel;

		try {
			rel = firstNode.createRelationshipTo(secondNode, relationshipType);
			tx.success();
		} finally {
			tx.finish();
		}
		return rel;
	}

	/**
	 * Attempts to look up a {@link Relationship} of a given type, between two
	 * {@link Node nodes}. If such relationship exists, it is returned,
	 * otherwise method returns <code>null</code>.
	 * 
	 * @param firstNode the first node sharing the relationship
	 * @param secondNode the second node sharing the relationship
	 * @param relationshipType required type of the relationship
	 * @return relationship between two given nodes. If no such relationship
	 *         exists, <code>null</code> is returned.
	 * @see #getRelationship(Node, Node, RelationshipType)
	 */
	protected Relationship getSingleRelationship(final Node firstNode, final Node secondNode,
			final RelationshipType relationshipType) {

		for (Relationship rel : firstNode.getRelationships(relationshipType)) {
			if (rel.getOtherNode(firstNode).equals(secondNode)) {
				return rel;
			}
		}
		return null;
	}

	/**
	 * Attempts to look up all {@link Relationship relationships} of a given
	 * type, between two {@link Node nodes}. If no relationships of given type
	 * exist between the two nodes, empty list is returned.
	 * 
	 * @param firstNode the first node sharing the relationships
	 * @param secondNode the second node sharing the relationships
	 * @param relationshipType required type of relationships
	 * @return list of relationships between two given nodes. If no such
	 *         relationship exists, empty list is returned.
	 * @see #getSingleRelationship(Node, Node, RelationshipType)
	 */
	protected ArrayList<Relationship> getRelationship(final Node firstNode, final Node secondNode,
			final RelationshipType relationshipType) {

		ArrayList<Relationship> relations = new ArrayList<Relationship>();

		for (Relationship rel : firstNode.getRelationships(relationshipType)) {
			if (rel.getOtherNode(firstNode).equals(secondNode)) {
				relations.add(rel);
			}
		}
		return relations;
	}
	
	protected void addToIndex(Index<Node> index, Node node, String key, Object value) {
		
		Transaction tx = graphDb.beginTx();
		
		try {
			index.add(node, key, value);
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	protected void deleteRelationship(Relationship relationship) {
		
		Transaction tx = graphDb.beginTx();
		
		try {
			relationship.delete();
			tx.success();
		} finally {
			tx.finish();
		}
	}
}