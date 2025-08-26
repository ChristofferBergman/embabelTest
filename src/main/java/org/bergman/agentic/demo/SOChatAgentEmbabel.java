package org.bergman.agentic.demo;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Implementation of the tools for our agent, tools that all delegate directly to Cypher and Neo4j
 */
@Component
public class SOChatAgentEmbabel {
	private final Neo4jConnection neo4j;

	protected SOChatAgentEmbabel(Neo4jConnection neo4j) {
		this.neo4j = neo4j;
	}
	
	@Tool(description = 
			"""
			Find relevant questions (topics) in the graph based on vector search on the question the user asked (the prompt)
			""")
	public Object findRelevantQuestions(
			@ToolParam(description = "The question as asked by the user") String userQuestion
			) throws Exception {
		return neo4j.getRelevantQuestions(userQuestion);
	}

	@Tool(description = 
			"""
			For a specific question (topic), get all posts in that thread (the question itself and all answers).
			The result is unsorted, but there is a created field with when it was posted.
			""")
	public Object retrieveThread(
			@ToolParam(description = "The id of the question/topic to get the thread for") String questionId
			) throws Exception {
		return neo4j.getThread(questionId);
	}

	@Tool(description = 
			"""
			For a specific question (topic), get the answer that has been indicated as the accepted answer
			(if there is one, otherwise it returns a string that says 'No accepted answer')
			""")
	public Object retrieveAcceptedAnswer(
			@ToolParam(description = "The id of the question/topic to get the accepted answer for") String questionId
			) throws Exception {
		var result = neo4j.getAcceptedAnswer(questionId);
		if (result == null) {
			return "No accepted answer";
		}
		return result;
	}

	@Tool(description = 
			"""
			Fetch all comments for a specific post (question or answer).
			This may be an empty list if there are no comments.
			""")
	public Object retrieveComments(
			@ToolParam(description = "The id of the post to get the comments for") String postId
			) throws Exception {
		return neo4j.getComments(postId);
	}

	@Tool(description = 
			"""
			Get the user that posted a question, an answer or a comment.
			""")
	public Object getUser(
			@ToolParam(description = "The id of the post (question or answer) or comment for which to get the user who posted.") String entityId
			) throws Exception {
		return neo4j.getUser(entityId);
	}

	@Tool(description = 
			"""
			Get all posts (questions and answers) posted by a specific user.
			""")
	public Object getUserPosts(
			@ToolParam(description = "The user id to get the posted posts for.") String userId
			) throws Exception {
		return neo4j.getUserPosts(userId);
	}

	@Tool(description = 
			"""
			Get all comments posted by a specific user.
			""")
	public Object getUserComments(
			@ToolParam(description = "The user id to get the posted comments for.") String userId
			) throws Exception {
		return neo4j.getUserComments(userId);
	}

	@Tool(description = 
			"""
			Get the post that an answer or a comment was posted on.
			If there is no parent (i.e. the post was a question) it return the string 'No parent'
			""")
	public Object getParentPost(
			@ToolParam(description = "The id of the post (answer) or comment") String entityId
			) throws Exception {
		var result = neo4j.getParentPost(entityId);
		if (result == null) {
			return "No parent";
		}
		return result;
	}
}
