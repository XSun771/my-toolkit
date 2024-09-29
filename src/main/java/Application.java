import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author sun-yixin
 */
@Slf4j
public class Application {

    private static final Charset TASKLIST_CHARSET = Charset.forName("GB2312");
    private static AtomicReference<String> PID = new AtomicReference<>("");

    public static void main(String[] args) throws InterruptedException {
        final String programName = StringUtils.firstNonBlank(ArrayUtils.isEmpty(args) ? "" : args[0], "War3.exe");
        final Application application = new Application();
        while (true) {
            log.info("Application With-War3 starts ...");
            application.getPID(programName).map((ignored) -> {
                application.start(programName);
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException ignoredExp) {
                }
                application.start(programName);
                return ignored;
            }).or(() -> {
                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException ignored) {
                }
                return Optional.empty();
            });
        }
    }

    private void start(String programName) {
        this.getPID(programName).ifPresentOrElse(pid -> {
            if (!StringUtils.equalsIgnoreCase(PID.get(), pid)) {
                PID.updateAndGet(v -> pid);
                try {
                    new ProcessBuilder("D:\\myGame\\war3_show_all.exe").start();
                    log.info("war3_show_all.exe starts");
                } catch (IOException e) {
                    log.error("failed to start war3_show_all.exe", e);
                }
                getPID("war3_repair.exe").ifPresentOrElse(ignored -> {
                    log.info("war3_repair.exe has started");
                }, () -> {
                    try {
                        new ProcessBuilder("D:\\myGame\\war3_repair.exe").start();
                        log.info("war3_repair.exe starts");
                    } catch (IOException e) {
                        log.error("failed to start war3_repair.exe", e);
                    }
                });
            } else {
                log.info("Application already launched tools for War3.exe");
            }
        }, () -> {
            log.info("War3.exe doesn't exist");
        });
    }

    private Optional<String> getPID(String programName) {
        final String command = String.format("%s/system32/tasklist.exe /FI \"IMAGENAME eq %s\"", System.getenv("windir"),
                programName);
        final Process tasklistProcess;
        try {
            tasklistProcess = Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            log.error("failed to execute {}", command, e);
            return Optional.empty();
        }

        try (final InputStream taskListStdout = tasklistProcess.getInputStream();
             final BufferedReader processStOutput = new BufferedReader(new InputStreamReader(taskListStdout, TASKLIST_CHARSET));
             final InputStream processStdErr = new BufferedInputStream(tasklistProcess.getErrorStream())) {

            String line = null;
            while ((line = processStOutput.readLine()) != null) {
                // War3.exe                 5084 Console                    8    374,968 K
                // war3_repair.exe              25740 Console                    8     70,432 K
                final String[] taskListItem = StringUtils.split(line);
                if (taskListItem.length >= 2 && StringUtils.equalsIgnoreCase(programName, taskListItem[0])) {
                    return Optional.of(taskListItem[1]);
                }
            }

            final String errorMessage = new String(processStdErr.readAllBytes(), TASKLIST_CHARSET);
            if (StringUtils.isNotBlank(errorMessage)) {
                log.error("tasklist.exe stderr: {}", errorMessage);
            } else {
                log.error("result of {} is empty", command);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("failed to read output stream of tasklist.exe", e);
            return Optional.empty();
        }
    }
}
