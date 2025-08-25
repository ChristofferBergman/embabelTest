package org.bergman.agentic.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.embabel.agent.config.annotation.EnableAgentShell;
import com.embabel.agent.config.annotation.EnableAgents;
import com.embabel.agent.config.annotation.LoggingThemes;

@SpringBootApplication
@EnableAgentShell
@EnableAgents(loggingTheme = LoggingThemes.STAR_WARS)
public class AgentApp {
    public static void main(String[] args) {
        SpringApplication.run(AgentApp.class, args);
    }
}
