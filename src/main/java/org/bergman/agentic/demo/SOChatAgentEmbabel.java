package org.bergman.agentic.demo;

import org.springframework.stereotype.Component;
import org.springframework.ai.chat.client.ChatClient;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;

@Component
@Agent(description =
		"""
		You are assisting a development team with questions on their specific development environment.
		For this you have a graph which is an export from Stack Overflow for teams. It has posts and comments on those posts.
		The original post is usually a question, and the other posts are answers on that question.
		All posts and comments has a link to the user that posted them.
		"""
		)
public class SOChatAgentEmbabel {

    private final ChatClient.Builder chatClientBuilder;
    private final SOChatTools tools;

    public SOChatAgentEmbabel(ChatClient.Builder chatClientBuilder, SOChatTools tools) {
        this.chatClientBuilder = chatClientBuilder;
        this.tools = tools;
    }

    @AchievesGoal(
            description = """
                Answer the user's question using tools that read from a Neo4j graph
                of Stack Overflow for Teams (posts, comments, users). Prefer accepted
                answers and higher-scored posts. If the graph does not contain enough
                info, say "I don't know".
                """,
            examples = {
                "How do I fix the local dev Docker error X?",
                "Who wrote the accepted answer about service Y timeouts?"
            }
    )
    @Action
    public String answer(String userQuestion) {
        String system = """
            You are SOChat, an internal assistant for Neo4j engineers.
            You have tools to fetch Stack Overflow for Teams content from a Neo4j graph.
            Use tools to gather evidence before answering. Prefer accepted answers and higher
            scored posts. If the graph lacks the information, reply exactly with: I don't know.
            Keep replies concise and include post IDs in parentheses when you cite a specific post.
            """;

        return chatClientBuilder.build()
                .prompt()
                .system(system)
                .user(userQuestion)
                .tools(this.tools)
                .call()
                .content();
    }
}
