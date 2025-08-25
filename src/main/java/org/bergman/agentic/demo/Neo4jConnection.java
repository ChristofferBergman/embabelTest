package org.bergman.agentic.demo;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.SessionConfig;
import org.springframework.stereotype.Component;

@Component
public class Neo4jConnection {
	private static final String DB_NAME = "neo4j";

	private final Driver driver;

	public Neo4jConnection(Driver driver) {
		this.driver = driver;
	}

	public List<Map<String, Object>> getRelevantQuestions(String userQuestion) {
		var query = new Query(
				"""
				WITH genai.vector.encode($question, "OpenAI", {token: $apiKey}) AS embedding
				CALL db.index.vector.queryNodes('post_embeddings', 2, embedding) YIELD node AS p, score AS pscore
				MATCH (p)((:Post)-[:PARENT]->(:Post))*(parent)
				WITH embedding, collect(parent) AS parents
				CALL db.index.vector.queryNodes('title_embeddings', 2, embedding) YIELD node AS q, score AS qscore
				WITH parents, collect(q) AS posts
				UNWIND parents+posts AS question
				RETURN DISTINCT question {.score, .title, .body, id: elementId(question), created: toString(question.created)}
				""",
				Map.of("question", userQuestion, "apiKey", System.getenv("OPENAI_API_KEY")));

		try (var session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
			var record = session.executeRead(tx -> tx.run(query).list());
			return record.stream().map(r -> r.get("question").asMap()).toList();
		}
	}
	
	public List<Map<String, Object>> getThread(String questionId) {
		var query = new Query(
				"""
				MATCH (q:Post) WHERE elementId(q) = $question
				MATCH path = (q)((:Post)<-[:PARENT]-(:Post))*
				UNWIND nodes(path) AS post
				RETURN post {.score, .body, .postType, id: elementId(post), created: toString(post.created)}
				""",
				Map.of("question", questionId));

		try (var session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
			var record = session.executeRead(tx -> tx.run(query).list());
			return record.stream().map(r -> r.get("post").asMap()).toList();
		}
	}
	
	public Map<String, Object> getAcceptedAnswer(String questionId) {
		var query = new Query(
				"""
				MATCH (q:Post) WHERE elementId(q) = $question
				OPTIONAL MATCH (q)-[:ACCEPTED_ANSWER]->(post:Post)
				RETURN post {.score, .body, .postType, id: elementId(post), created: toString(post.created)}
				""",
				Map.of("question", questionId));

		try (var session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
			var record = session.executeRead(tx -> tx.run(query).single());
			return record.get("post").isNull() ? null : record.get("post").asMap();
		}
	}
	
	public List<Map<String, Object>> getComments(String postId) {
		var query = new Query(
				"""
				MATCH (p:Post) WHERE elementId(p) = $post
				MATCH (p)<-[:ON_POST]-(comment:Comment)
				RETURN comment {.text, id: elementId(comment), created: toString(comment.created)}
				""",
				Map.of("post", postId));

		try (var session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
			var record = session.executeRead(tx -> tx.run(query).list());
			return record.stream().map(r -> r.get("comment").asMap()).toList();
		}
	}
	
	public Map<String, Object> getUser(String entityId) {
		var query = new Query(
				"""
				MATCH (e) WHERE elementId(e) = $entity
				MATCH (e)-[]->(user:User)
				RETURN user {.reputation, .bronzeBadges, .silverBadges, .goldBadges, name: user.displayName, id: elementId(user), created: toString(user.created)}
				""",
				Map.of("entity", entityId));

		try (var session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
			var record = session.executeRead(tx -> tx.run(query).single());
			return record.get("user").asMap();
		}
	}
	
	public List<Map<String, Object>> getUserPosts(String userId) {
		var query = new Query(
				"""
				MATCH (u:User) WHERE elementId(u) = $user
				MATCH (post:Post)-[:POSTED_BY]->(u)
				RETURN post {.score, .body, .postType, id: elementId(post), created: toString(post.created)}
				""",
				Map.of("user", userId));

		try (var session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
			var record = session.executeRead(tx -> tx.run(query).list());
			return record.stream().map(r -> r.get("post").asMap()).toList();
		}
	}
	
	public List<Map<String, Object>> getUserComments(String userId) {
		var query = new Query(
				"""
				MATCH (u:User) WHERE elementId(u) = $user
				MATCH (comment:Comment)-[:COMMENTED_BY]->(u)
				RETURN comment {.text, id: elementId(comment), created: toString(comment.created)}
				""",
				Map.of("user", userId));

		try (var session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
			var record = session.executeRead(tx -> tx.run(query).list());
			return record.stream().map(r -> r.get("comment").asMap()).toList();
		}
	}
	
	public Map<String, Object> getParentPost(String entityId) {
		var query = new Query(
				"""
				MATCH (e) WHERE elementId(e) = $entity
				OPTIONAL MATCH (e)-[:PARENT|POST]->(post:Post)
				RETURN post {.score, .body, .postType, id: elementId(post), created: toString(post.created)}
				""",
				Map.of("entity", entityId));

		try (var session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
			var record = session.executeRead(tx -> tx.run(query).single());
			return record.get("post").isNull() ? null : record.get("post").asMap();
		}
	}
}
