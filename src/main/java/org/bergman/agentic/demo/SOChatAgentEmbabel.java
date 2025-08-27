package org.bergman.agentic.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;

/**
 * Implementation of the tools for our agent, tools that all delegate directly to Cypher and Neo4j
 */
@ShellComponent
public class SOChatAgentEmbabel {
	private final Neo4jConnection neo4j;
	private final Ai ai;

	protected SOChatAgentEmbabel(Neo4jConnection neo4j, Ai ai) {
		this.neo4j = neo4j;
		this.ai = ai;
	}
	
	@Tool(description = 
			"""
			Find relevant questions (topics) in the graph based on vector search on the question the user asked (the prompt)
			""")
	public List<Map<String, Object>> findRelevantQuestions(
			@ToolParam(description = "The question as asked by the user") String userQuestion
			) throws Exception {
		return neo4j.getRelevantQuestions(userQuestion);
	}

	@Tool(description = 
			"""
			For a specific question (topic), get all posts in that thread (the question itself and all answers).
			The result is unsorted, but there is a created field with when it was posted.
			""")
	public List<Map<String, Object>> retrieveThread(
			@ToolParam(description = "The id of the question/topic to get the thread for") String questionId
			) throws Exception {
		return neo4j.getThread(questionId);
	}

	@Tool(description = 
			"""
			For a specific question (topic), get the answer that has been indicated as the accepted answer
			(if there is one, otherwise it returns a string that says 'No accepted answer')
			""")
	public Map<String, Object> retrieveAcceptedAnswer(
			@ToolParam(description = "The id of the question/topic to get the accepted answer for") String questionId
			) throws Exception {
		var result = neo4j.getAcceptedAnswer(questionId);
		if (result == null) {
			return new HashMap<>();
		}
		return result;
	}

	@Tool(description = 
			"""
			Fetch all comments for a specific post (question or answer).
			This may be an empty list if there are no comments.
			""")
	public List<Map<String, Object>> retrieveComments(
			@ToolParam(description = "The id of the post to get the comments for") String postId
			) throws Exception {
		return neo4j.getComments(postId);
	}

	@Tool(description = 
			"""
			Get the user that posted a question, an answer or a comment.
			""")
	public Map<String, Object> getUser(
			@ToolParam(description = "The id of the post (question or answer) or comment for which to get the user who posted.") String entityId
			) throws Exception {
		return neo4j.getUser(entityId);
	}

	@Tool(description = 
			"""
			Get all posts (questions and answers) posted by a specific user.
			""")
	public List<Map<String, Object>> getUserPosts(
			@ToolParam(description = "The user id to get the posted posts for.") String userId
			) throws Exception {
		return neo4j.getUserPosts(userId);
	}

	@Tool(description = 
			"""
			Get all comments posted by a specific user.
			""")
	public List<Map<String, Object>> getUserComments(
			@ToolParam(description = "The user id to get the posted comments for.") String userId
			) throws Exception {
		return neo4j.getUserComments(userId);
	}

	@Tool(description = 
			"""
			Get the post that an answer or a comment was posted on.
			If there is no parent (i.e. the post was a question) it return the string 'No parent'
			""")
	public Map<String, Object> getParentPost(
			@ToolParam(description = "The id of the post (answer) or comment") String entityId
			) throws Exception {
		var result = neo4j.getParentPost(entityId);
		if (result == null) {
			return new HashMap<>();
		}
		return result;
	}
	
	@ShellMethod
    public String answer(String userQuestion) {
        return ai
                .withLlm(LlmOptions.withDefaultLlm().withTemperature(.8))
                .withToolObject(this)
                .generateText(
                		"""
                		You are assisting a development team with questions on their specific development environment.
                		For this you have a graph which is an export from Stack Overflow for teams. It has posts and comments on those posts.
                		The original post is usually a question, and the other posts are answers on that question.
                		All posts and comments has a link to the user that posted them. The question is:
                		""" + userQuestion
                		);
    }
}
