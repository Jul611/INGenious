package com.ing.engine.core;

import com.ing.datalib.component.Project;
import com.ing.datalib.testdata.TestDataFactory;
import com.ing.engine.cli.LookUp;

import com.ing.engine.constants.FilePath;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.execution.run.ProjectRunner;

import com.ing.engine.reporting.SummaryReport;
import com.ing.engine.reporting.impl.ConsoleReport;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.engine.support.reflect.MethodExecutor;
import com.ing.util.encryption.Encryption;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.drivers.WebDriverFactory;
import com.ing.engine.drivers.SAPSessionCreation;
import java.time.Instant;

public class Control {

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%1$tmS %1$tz [%4$-4s] %2$s:%5$s%6$s%n");
    }
    private static final Logger LOG = Logger.getLogger(Control.class.getName());

    public static SummaryReport ReportManager;
    public Boolean executionFinished = false;
    public static ProjectRunner exe;
    public static String triggerId;
    public static String executionStartTime;

    private static PlaywrightDriverCreation playwrightDriver;

    private static WebDriverCreation webDriver;

    private static SAPSessionCreation sapSession;

    private static void start() {
        do {
            Control control = new Control();
            control.startRun();
            control.resetAll();
        } while (exe.retryExecution());
        ConsoleReport.reset();

    }

    public static void call(Project project) throws UnCaughtException {
        RunManager.init();
        exe = ProjectRunner.load(project);
        start();
    }

    public static void call() throws UnCaughtException {
        RunManager.init();
        if (exe == null) {
            exe = ProjectRunner.load(RunManager.getGlobalSettings().getProjectPath());
        }
        start();
    }

    public static Project getCurrentProject() {
        if (exe != null) {
            return exe.getProject();
        }
        return null;
    }

    void resetAll() {

        exe.afterExecution(ReportManager.isPassed());
        SystemDefaults.resetAll();
        SummaryReport.reset();
        ReportManager = null;
        RunManager.clear();
    }

    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (!executionFinished) {
                    endExecution();

                    ConsoleReport.reset();

                }
            }
        });
    }

    private void initRun() throws Exception {
        executionFinished = false;
        addShutDownHook();
        FilePath.initDateTime();
        MethodExecutor.init();
        ConsoleReport.init();
        SystemDefaults.printSystemInfo();
        
        // Print INGenious ASCII Banner
        printExecutionBanner();
        
        WebDriverFactory.initDriverLocation(exe.getProject().getProjectSettings());
        RunManager.loadRunManager();
        ReportManager = new SummaryReport();
        triggerId = UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 15);
        executionStartTime = String.valueOf(Instant.now());
    }

    private void startRun() {
        try {
            initRun();
            TMIntegration.init(ReportManager);
            ReportManager.createReport(DateTimeUtils.DateTimeNow(), RunManager.queue().size());
            ThreadPool threadPool = new ThreadPool(
                    exe.getExecSettings().getRunSettings().getThreadCount(),
                    exe.getExecSettings().getRunSettings().getExecutionTimeOut(),
                    exe.getExecSettings().getRunSettings().isGridExecution());
            
            while (!RunManager.queue().isEmpty() && !SystemDefaults.stopExecution.get()) {
                Task t = null;
                try {
                    RunContext currentContext = RunManager.queue().remove();
                    t = new Task(currentContext);
                    threadPool.execute(t, currentContext.Browser);
                } catch (Exception ex) {
                    Logger.getLogger(Control.class.getName()).log(Level.SEVERE, null, ex);
                    if (t != null) {
                        t.playwrightDriver.closeBrowser();
                    }
                }
            }
            threadPool.shutdownExecution();

            if (threadPool.awaitTermination(exe.getExecSettings()
                    .getRunSettings().getExecutionTimeOut(), TimeUnit.MINUTES)) {
            } else {
                Logger.getLogger(Control.class.getName()).log(Level.SEVERE, "Execution stopped due to Timeout [{0}]",
                        exe.getExecSettings().getRunSettings().getExecutionTimeOut());
                threadPool.shutdownNow();
                SystemDefaults.stopExecution.set(true);
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            if (ReportManager != null) {
                SystemDefaults.reportComplete.set(false);
                ReportManager.updateTestCaseResults("[Unknown Error]", "---", ex.getMessage(), "", "Unknown", "Unknown",
                        Status.FAIL, "");
            }
        } finally {

            while (SystemDefaults.reportComplete.get()) {

                SystemDefaults.pollWait();

            }

            endExecution();

        }
    }

    static PlaywrightDriverCreation getPlaywrightDriver() {
        return playwrightDriver;
    }

    static WebDriverCreation getWebDriver() {
        return webDriver;
    }

    static SAPSessionCreation getSapSession() {
        return sapSession;
    }
    
    static void setPlaywrightDriver(PlaywrightDriverCreation Driver) {
        playwrightDriver = Driver;
    }

    static void setSapSession(SAPSessionCreation session) {
        sapSession = session;
    }

    static void setWebDriver(WebDriverCreation Driver) {
        webDriver = Driver;
    }

    private void endExecution() {
        executionFinished = true;
        System.out.println("Run Finished on " + new Date().toString());
        try {
            if (ReportManager != null) {
                ReportManager.finalizeReport();
                if (ReportManager.sync != null) {
                    ReportManager.sync.disConnect();
                }

            }

            if (playwrightDriver != null) {
                playwrightDriver.closeBrowser();
                playwrightDriver.playwright.close();
            } 
           else if(webDriver != null)
            {
                webDriver.driver.quit();
            }

        } catch (Exception ex) {
            Logger.getLogger(Control.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void initDeps() {
        TestDataFactory.load();
        MethodInfoManager.load();
        Encryption.getInstance();
    }

    /**
     * Determines if arguments are for the new CLI (picocli-based).
     * New CLI commands: project, scenario, testcase, action, run, report, config, server, shell
     * Legacy CLI uses: -run, -project_location, -scenario, etc.
     */
    private static boolean isNewCLICommand(String[] args) {
        if (args == null || args.length == 0) return false;
        
        String firstArg = args[0].toLowerCase();
        
        // New CLI subcommands
        String[] newCommands = {
            "project", "scenario", "testcase", "action", "actions", 
            "run", "report", "config", "server", "shell", 
            "interactive", "repl", "help", "--help", "-h", "--version"
        };
        
        for (String cmd : newCommands) {
            if (firstArg.equals(cmd)) {
                return true;
            }
        }
        
        return false;
    }

    public static void main(String[] args) throws UnCaughtException {
        initDeps();
        
        if (args != null && args.length > 0) {
            // Check if new CLI command
            if (isNewCLICommand(args)) {
                // Use new Picocli-based CLI
                int exitCode = com.ing.engine.cli.INGeniousCLI.run(args);
                if (exitCode != 0) {
                    System.exit(exitCode);
                }
            } else {
                // Legacy CLI handling
                LookUp.exe(args);
            }
        } else {
            // No args - show CLI help with banner
            int exitCode = com.ing.engine.cli.INGeniousCLI.run(new String[0]);
            System.exit(exitCode);
        }
    }

    /**
     * Print INGenious ASCII banner at execution start
     */
    private void printExecutionBanner() {
        String projectName = exe.getProject() != null ? exe.getProject().getName() : "Unknown";
        String browser = RunManager.getGlobalSettings().getBrowser();
        if (browser == null || browser.isEmpty()) {
            browser = "Default";
        }
        String platform = System.getProperty("os.name", "Unknown");
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                              ║");
        System.out.println("║   ██╗███╗   ██╗ ██████╗ ███████╗███╗   ██╗██╗ ██████╗ ██╗   ██╗███████╗      ║");
        System.out.println("║   ██║████╗  ██║██╔════╝ ██╔════╝████╗  ██║██║██╔═══██╗██║   ██║██╔════╝      ║");
        System.out.println("║   ██║██╔██╗ ██║██║  ███╗█████╗  ██╔██╗ ██║██║██║   ██║██║   ██║███████╗      ║");
        System.out.println("║   ██║██║╚██╗██║██║   ██║██╔══╝  ██║╚██╗██║██║██║   ██║██║   ██║╚════██║      ║");
        System.out.println("║   ██║██║ ╚████║╚██████╔╝███████╗██║ ╚████║██║╚██████╔╝╚██████╔╝███████║      ║");
        System.out.println("║   ╚═╝╚═╝  ╚═══╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝╚═╝ ╚═════╝  ╚═════╝ ╚══════╝      ║");
        System.out.println("║                                                                              ║");
        System.out.println("║                    🚀 Test Automation Framework v2.3.1                       ║");
        System.out.println("║                                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  📁 Project:  " + projectName);
        System.out.println("  🌐 Browser:  " + browser);
        System.out.println("  💻 Platform: " + platform);
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

}
