package com.atm.integration;

import com.atm.application.ATMFacade;
import com.atm.cli.CLIHandler;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

public class ATMCliIntegrationTest {
  private TestCLIHandler cliHandler;

  /** Executor service to run the CLI in a separate thread */
  private ExecutorService executorService;

  private ATMFacade atmFacade;
  private Future<?> cliFuture;

  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
  private final PrintStream standardOut = System.out;

  private static class TestCLIHandler implements CLIHandler {

    /**
     * Queue of inputs to be read by the CLI The inputs are consumed by the CLI in a FIFO manner Can
     * handle timout safely
     */
    private final BlockingQueue<String> inputs = new LinkedBlockingQueue<>();

    private final List<String> outputs = new ArrayList<>();

    void queueInputs(List<String> commands) {
      inputs.addAll(commands);
    }

    @Override
    public String readLine() {
      try {
        String input = inputs.poll(5, TimeUnit.SECONDS);
        return input != null ? input : "exit";
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return "exit";
      }
    }

    @Override
    public void print(String message) {
      outputs.add(message);
    }

    @Override
    public void printError(String message) {
      outputs.add("Error: " + message);
      System.out.println("Error: " + message);
    }

    @Override
    public void printSuccess(String message) {
      outputs.add("Success: " + message);
    }
  }

  @BeforeAll
  static void setUpEnvironment() {
    System.setProperty("DB_URL", "jdbc:postgresql://test-db:5432/atm_cli_test");
    System.setProperty("DB_USERNAME", "postgres");
    System.setProperty("DB_PASSWORD", "postgres");
  }

  @BeforeEach
  void setUp() {
    System.setOut(new PrintStream(outputStreamCaptor));
    cliHandler = new TestCLIHandler();
    executorService = Executors.newSingleThreadExecutor();
    atmFacade = new ATMFacade(cliHandler);
  }

  @AfterEach
  void tearDown() {
    System.setOut(standardOut);
    if (cliFuture != null) {
      atmFacade.close();
      executorService.shutdownNow();
      try {
        executorService.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Test
  void testFullUserFlow() throws Exception {
    cliFuture = executorService.submit(() -> atmFacade.start());

    List<String> commands =
        List.of(
            "register testUser KentangAjaib123",
            "login testUser kentangAjaib123",
            "login testUser KentangAjaib123",
            "login testUser KentangAjaib123",
            "withdraw 1000",
            "withdraw -100",
            "withdraw 1",
            "withdraw asd",
            "deposit -1",
            "deposit asd",
            "deposit 1000",
            "withdraw 500",
            "transfer bobi 100",
            "logout",
            "register bobi kentangjaib",
            "register bobi Asd123",
            "register bobi KentangAjaib123",
            "login bobi",
            "login bobi KentangAjaib123",
            "login bobi KentangAjaib123",
            "balance",
            "deposit 1000",
            "transfer testUser 100",
            "balance",
            "transfer alice2 100",
            "logout");

    cliHandler.queueInputs(commands);

    Thread.sleep(1000);

    String output = outputStreamCaptor.toString();
    List<String> outputLines = List.of(output.split("\n"));

    System.setOut(standardOut);
    System.out.println("Captured outputs:");
    outputLines.forEach(line -> System.out.println("OUTPUT => " + line));

    assertOutputContains(outputLines, "Registration successful with username: testUser");
    assertOutputContains(outputLines, "Error: Invalid Credentials");
    assertOutputContains(outputLines, "Hello, testUser");
    assertOutputContains(outputLines, "Error: User already logged in. Please logout first.");
    assertOutputContains(outputLines, "Error: Insufficient balance");
    assertOutputContains(outputLines, "Error: Invalid amount, must be grater than 1");
    assertOutputContains(outputLines, "Error: Invalid amount, must be grater than 1");
    assertOutputContains(outputLines, "Error: Invalid amount format");
    assertOutputContains(outputLines, "Error: Invalid amount, must be grater than 1");
    assertOutputContains(outputLines, "Error: Invalid amount format");
    assertOutputContains(outputLines, "Deposit successful. New balance: $1000.0000");
    assertOutputContains(outputLines, "Withdraw successful. New balance: $500.0000");
    assertOutputContains(outputLines, "Error: User not found");
    assertOutputContains(outputLines, "Goodbye, testUser");
    assertOutputContains(
        outputLines,
        "Error: Password must contain at least one uppercase letter, one lowercase letter, and one number.");
    assertOutputContains(outputLines, "Error: Password must be at least 8 characters long.");
    assertOutputContains(outputLines, "Registration successful with username: bobi");
    assertOutputContains(outputLines, "Error: Usage: login <username> <password>");
    assertOutputContains(outputLines, "Hello, bobi");
    assertOutputContains(outputLines, "Error: User already logged in. Please logout first.");
    assertOutputContains(outputLines, "Balance: $0.0000");
    assertOutputContains(outputLines, "Deposit successful. New balance: $1000.0000");
    assertOutputContains(outputLines, "Transfer successful.");
    assertOutputContains(outputLines, "Balance: $900.0000");
    assertOutputContains(outputLines, "Transfer successful.");
    assertOutputContains(outputLines, "Error: User not found");
    assertOutputContains(outputLines, "Goodbye, bobi");
  }

  private void assertOutputContains(List<String> outputs, String expected) {
    Assertions.assertTrue(
        outputs.stream().anyMatch(output -> output.contains(expected)),
        "Expected output containing '" + expected + "' not found in: " + outputs);
  }
}
