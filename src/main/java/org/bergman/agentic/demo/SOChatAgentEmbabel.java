package org.bergman.agentic.demo;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <p>Purpose:
 * Assist a development team with questions about their specific dev environment,
 * using a Neo4j graph exported from Stack Overflow for Teams. The graph contains
 * posts (questions/answers) and comments; answers may be linked via {@code PARENT}
 * and one may be linked from a question via {@code ACCEPTED_ANSWER}. All posts
 * and comments are linked to the user who wrote them.</p>
 *
 * <p>Planning guidance:
 * Prefer accepted answers when present and consider scores/time where helpful.
 * Use the actions below to (a) locate candidate questions, (b) traverse threads,
 * (c) fetch accepted answers, comments, and (d) pivot via users.</p>
 *
 * <p>Inputs/outputs:
 * The actions return simple JSON-friendly maps/lists, matching the projections
 * used by the Neo4j connection.</p>
 */
@Agent(
		description = """
				You assist a development team with questions on their specific development environment.
				You have a Neo4j graph exported from Stack Overflow for Teams: posts (questions/answers)
				and comments; answers relate via PARENT and a question may have an ACCEPTED_ANSWER.
				Posts/comments are linked to their author. Prefer accepted answers when possible.
				"""
		)
@Component
public class SOChatAgentEmbabel {

	private final Neo4jConnection neo4j;

	public SOChatAgentEmbabel(Neo4jConnection neo4j) {
		this.neo4j = neo4j;
	}

	// ───────────────────────────── Tools ─────────────────────────────

	/**
	 * Find potentially relevant question posts (topics) by running vector search
	 * using the user’s question text as the query. This returns candidate questions
	 * you can use as starting points for further traversal.
	 *
	 * @param userQuestion the question exactly as asked by the user
	 * @return a list of candidate question/topic maps (includes id/title/body/created/score)
	 * @throws Exception on data access errors
	 */
	@Action(description = "Find relevant questions (topics) using vector search on the user's prompt")
	public List<Map<String, Object>> findRelevantQuestions(String userQuestion) throws Exception {
		return neo4j.getRelevantQuestions(userQuestion);
	}

	/**
	 * For a given question/topic node, retrieve all posts in the thread
	 * (the original question plus all answers). The list is unsorted; each
	 * post map contains a {@code created} value you can use for ordering.
	 *
	 * @param questionId the id of the question/topic whose thread should be returned
	 * @return a list of post maps for the full thread (question + answers)
	 * @throws Exception on data access errors
	 */
	@Action(description = "For a question/topic, return all posts in that thread (question + answers), unsorted; each has a 'created' field.")
	public List<Map<String, Object>> retrieveThread(String questionId) throws Exception {
		return neo4j.getThread(questionId);
	}

	/**
	 * Look up the accepted answer for a given question/topic. If the question
	 * has no accepted answer, returns the literal string {@code "No accepted answer"}.
	 *
	 * @param questionId the id of the question/topic for which to fetch the accepted answer
	 * @return the accepted-answer post map, or the string "No accepted answer" if none exists
	 * @throws Exception on data access errors
	 */
	@Action(description = "For a question/topic, return the accepted answer if present; otherwise the string 'No accepted answer'.")
	public Object retrieveAcceptedAnswer(String questionId) throws Exception {
		var result = neo4j.getAcceptedAnswer(questionId);
		return (result == null) ? "No accepted answer" : result;
	}

	/**
	 * Fetch all comments associated with a specific post (question or answer).
	 * The returned list may be empty when no comments exist.
	 *
	 * @param postId the id of the post (question/answer) whose comments should be fetched
	 * @return a list of comment maps (possibly empty)
	 * @throws Exception on data access errors
	 */
	@Action(description = "Fetch all comments for a specific post (question or answer). May be empty.")
	public List<Map<String, Object>> retrieveComments(String postId) throws Exception {
		return neo4j.getComments(postId);
	}

	/**
	 * Get the user who authored a specific entity. The entity may be a post
	 * (question or answer) or a comment; this resolves the author accordingly.
	 *
	 * @param entityId the id of the post (question/answer) or comment whose author to fetch
	 * @return a user map for the author of the given entity
	 * @throws Exception on data access errors
	 */
	@Action(description = "Get the user who posted a question, answer, or comment.")
	public Map<String, Object> getUser(String entityId) throws Exception {
		return neo4j.getUser(entityId);
	}

	/**
	 * List all posts (questions and answers) written by a given user.
	 *
	 * @param userId the id of the user whose posts to return
	 * @return a list of post maps authored by the user
	 * @throws Exception on data access errors
	 */
	@Action(description = "List all posts (questions and answers) written by the given user.")
	public List<Map<String, Object>> getUserPosts(String userId) throws Exception {
		return neo4j.getUserPosts(userId);
	}

	/**
	 * List all comments written by a given user.
	 *
	 * @param userId the id of the user whose comments to return
	 * @return a list of comment maps authored by the user
	 * @throws Exception on data access errors
	 */
	@Action(description = "List all comments written by the given user.")
	public List<Map<String, Object>> getUserComments(String userId) throws Exception {
		return neo4j.getUserComments(userId);
	}

	/**
	 * Get the parent post for an answer or comment. If there is no parent
	 * (i.e., the entity itself is a top-level question), returns the literal
	 * string {@code "No parent"}.
	 *
	 * @param entityId the id of the answer or comment whose parent post to return
	 * @return the parent post map, or the string "No parent" if the entity is a question
	 * @throws Exception on data access errors
	 */
	@Action(description = "Get the parent post for an answer or comment, or 'No parent' if it is a top-level question.")
	public Object getParentPost(String entityId) throws Exception {
		var result = neo4j.getParentPost(entityId);
		return (result == null) ? "No parent" : result;
	}

	// ───────────────────────────── Goal ─────────────────────────────

	/**
	 * High-level goal: answer a user’s question using the Stack Overflow graph.
	 * The planner will sequence calls to the actions above (e.g., find candidates,
	 * check accepted answers, read comments) and synthesize a reply. Responses
	 * should cite accepted answers when available.
	 *
	 * @param userQuestion the end user’s question
	 * @return brief guidance to the planner; actual content will be composed from action results
	 */
	@AchievesGoal(description = "Answer the user's question using the Stack Overflow for Teams graph; prefer accepted answers and add brief evidence.")
	@Action
	public String answerFromStackOverflowGraph(String userQuestion) {
		// TODO: I haven't really grasped how goals works in Embabel
		// I really just want the LLM to plan the tools/actions based on the question
		// and what it gets back from previously called tools
		return "Use actions to find relevant threads; prefer accepted answers; include concise evidence.";
	}
}
