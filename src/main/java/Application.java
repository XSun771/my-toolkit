import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author sun-yixin
 */
@Slf4j
public class Application {

    //private static final Charset POWERSHELL_CHARSET = Charset.forName("GB2312");
    private static final Charset POWERSHELL_CHARSET = StandardCharsets.UTF_8;
    private static AtomicReference<String> PID = new AtomicReference<>("");

    public static void main(String[] args) throws InterruptedException {
        final String programName = StringUtils.firstNonBlank(ArrayUtils.isEmpty(args) ? "" : args[0], "War3.exe");
        final Application application = new Application();
        while (true) {
            log.info("Application With-War3 starts ...");
            boolean result = application.start(programName);
            TimeUnit.SECONDS.sleep(result ? 150 : 15);
        }
    }

    private boolean start(String programName) {
        return this.getPID(programName).map(pid -> {
            if (!StringUtils.equalsIgnoreCase(PID.get(), pid)) {
                PID.updateAndGet(v -> pid);
                try {
                    new ProcessBuilder("D:\\myGame\\war3_repair.exe").start();
                } catch (IOException e) {
                    log.error("failed to start war3_show_all.exe", e);
                    return false;
                }
                return getPID("War3字体重叠乱码修复工具.exe").map(ignored -> {
                    try {
                        new ProcessBuilder("D:\\myGame\\war3_show_all.exe").start();
                        return true;
                    } catch (IOException e) {
                        log.error("failed to start war3_show_all.exe", e);
                        return false;
                    }
                }).orElse(false);
            }
            return true;
        }).orElse(false);
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
        try (final BufferedReader processStOutput =
                     new BufferedReader(new InputStreamReader(tasklistProcess.getInputStream(), POWERSHELL_CHARSET));
             final InputStream processStdErr = new BufferedInputStream(tasklistProcess.getErrorStream())) {

            String line = null;
            while ((line = processStOutput.readLine()) != null) {
                log.info(line);
                // War3.exe                 5084 Console                    8    374,968 K
                // War3字体重叠乱码修复工具.    17948 Console                    8     66,824 K
                final String[] taskListItem = StringUtils.split(line);
                if (taskListItem.length >= 2 && StringUtils.equalsIgnoreCase(programName, taskListItem[0])) {
                    return Optional.of(taskListItem[1]);
                }
            }

            final String errorMessage = new String(processStdErr.readAllBytes(), POWERSHELL_CHARSET);
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
