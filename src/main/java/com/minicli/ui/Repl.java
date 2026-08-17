package com.minicli.ui;

import com.minicli.agent.core.ReActAgent;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/** JLine REPL：读取一行 → ReActAgent 处理（过程经 AgentDisplay 实时展示）→ 循环。 */
public final class Repl {

    private final ReActAgent agent;

    public Repl(ReActAgent agent) {
        this.agent = agent;
    }

    public void start() {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            System.out.println("👽: 哈欧波基尼，拉布可斯拉多");
            while (true) {
                String line;
                try {
                    line = reader.readLine("> ");
                } catch (EndOfFileException | UserInterruptException e) {
                    break;
                }
                if (line == null) {
                    break;
                }
                String input = line.trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    break;
                }
                try {
                    agent.run(input, new AgentDisplay(terminal));
                } catch (RuntimeException e) {
                    System.err.println("error> " + e.getMessage());
                }
            }
            System.out.println("bye");
        } catch (IOException e) {
            System.err.println("无法创建终端: " + e.getMessage());
        }
    }
}
