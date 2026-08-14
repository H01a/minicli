package com.minicli.ui;

import com.minicli.llm.DeepSeekClient;
import com.minicli.llm.StreamHandler;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/** JLine REPL：读取一行 → 调 LLM → 打印回答 → 循环；exit/quit 或 Ctrl-D/Ctrl-C 退出。 */
public final class Repl {

    private final DeepSeekClient client;

    public Repl(DeepSeekClient client) {
        this.client = client;
    }

    public void start() {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            System.out.println("minicli: 输入问题开始问答，exit 退出");
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
                    var writer = terminal.writer();
                    writer.print("assistant> ");
                    writer.flush();
                    client.askStream(input, new StreamHandler() {
                        @Override
                        public void onOutputDelta(String delta) {
                            writer.print(delta);
                            writer.flush();
                        }

                        @Override
                        public void onDone() {
                            writer.println();
                            writer.flush();
                        }
                    });
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
