package org.swacpay.scriptingengine;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScriptingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScriptingEngineApplication.class, args);
        try (Context context = Context.newBuilder("python")
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowIO(IOAccess.newBuilder()
                        .allowHostFileAccess(false)
                        .allowHostSocketAccess(false)
                        .build())
                .build()) {
            context.eval("python", "print('Hello from python')");
        }
        try (Context context = Context.newBuilder("js")
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowIO(IOAccess.newBuilder()
                        .allowHostFileAccess(false)
                        .allowHostSocketAccess(false)
                        .build())
                .build()) {
            context.eval("js", "console.log('Hello from JS')");
        }
    }
}
