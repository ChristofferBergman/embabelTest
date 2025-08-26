package org.bergman.agentic.demo;

import com.embabel.agent.api.common.Ai;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
record SOChatShell(Ai ai, SOChatAgentEmbabel agent) {

    @ShellMethod(
    		"""
    		You are assisting a development team with questions on their specific development environment.
    		For this you have a graph which is an export from Stack Overflow for teams. It has posts and comments on those posts.
    		The original post is usually a question, and the other posts are answers on that question.
    		All posts and comments has a link to the user that posted them.
    		"""
    		)
    public String answer(String userQuestion) {
        return ai
                .withDefaultLlm()
                .withToolObject(agent)
                .generateText(userQuestion);
    }
}
