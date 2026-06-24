package org.example.agent_qr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Agent-QR 应用程序启动类。
 * <p>
 * 基于 LangChain4j 的 RAG 企业内部知识库问答 Agent 系统。
 * </p>
 */
@SpringBootApplication
@ComponentScan(basePackages = "org.example.agent_qr")
public class AgentQrApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentQrApplication.class, args);
    }
}

