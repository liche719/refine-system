import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency-free HTTP benchmark for local Refine verification.
 *
 * <p>Run with Java 21 source-file mode. It measures client-observed latency through the gateway,
 * including authentication, routing, serialization, and downstream service work.</p>
 */
public final class ApiBenchmark {
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8080";
    private static final String DEFAULT_ACCOUNT = "demo@refine.local";
    private static final String DEFAULT_PASSWORD = "RefineDemo123";
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private ApiBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        Scenario scenario = Scenario.from(config.scenario());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();

        String accessToken = scenario.requiresAuthentication()
                ? login(client, config)
                : null;

        ExecutorService executor = Executors.newFixedThreadPool(config.concurrency());
        try {
            runRequests(client, executor, scenario, config, accessToken, config.warmupRequests());

            long benchmarkStartedAt = System.nanoTime();
            List<Result> results = runRequests(
                    client, executor, scenario, config, accessToken, config.requestCount());
            long elapsedNanos = System.nanoTime() - benchmarkStartedAt;

            BenchmarkSummary summary = BenchmarkSummary.from(config, scenario, results, elapsedNanos);
            String json = summary.toJson();
            System.out.println(json);
            if (config.output() != null) {
                writeOutput(config.output(), json);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static String login(HttpClient client, Config config) throws IOException, InterruptedException {
        String body = "{\"userAccount\":\"" + jsonEscape(config.account())
                + "\",\"userPassword\":\"" + jsonEscape(config.password()) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(config.baseUri().resolve("/api/userAccount/login"))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(response.body());
        if (response.statusCode() != 200 || !matcher.find()) {
            throw new IllegalStateException("Unable to obtain a benchmark access token. HTTP status: "
                    + response.statusCode());
        }
        return matcher.group(1);
    }

    private static List<Result> runRequests(
            HttpClient client,
            ExecutorService executor,
            Scenario scenario,
            Config config,
            String accessToken,
            int requestCount) throws Exception {
        if (requestCount == 0) {
            return List.of();
        }
        List<Future<Result>> futures = new ArrayList<>(requestCount);
        for (int index = 0; index < requestCount; index++) {
            futures.add(executor.submit(new RequestTask(client, scenario, config, accessToken)));
        }
        List<Result> results = new ArrayList<>(requestCount);
        for (Future<Result> future : futures) {
            results.add(future.get());
        }
        return results;
    }

    private static void writeOutput(Path output, String json) throws IOException {
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, json + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record RequestTask(HttpClient client, Scenario scenario, Config config, String accessToken)
            implements Callable<Result> {
        @Override
        public Result call() {
            long startedAt = System.nanoTime();
            try {
                HttpResponse<Void> response = client.send(buildRequest(), HttpResponse.BodyHandlers.discarding());
                return new Result(System.nanoTime() - startedAt, response.statusCode(), null);
            } catch (Exception exception) {
                return new Result(System.nanoTime() - startedAt, null, exception.getClass().getSimpleName());
            }
        }

        private HttpRequest buildRequest() {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUri().resolve(scenario.path()))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .header("Accept", "application/json");
            if (scenario.requiresAuthentication()) {
                builder.header("Authorization", "Bearer " + accessToken);
            }
            if (scenario == Scenario.LOGIN) {
                String body = "{\"userAccount\":\"" + jsonEscape(config.account())
                        + "\",\"userPassword\":\"" + jsonEscape(config.password()) + "\"}";
                return builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
            }
            return builder.GET().build();
        }
    }

    private record Result(long elapsedNanos, Integer statusCode, String errorType) {
        boolean successful() {
            return errorType == null && statusCode != null && statusCode >= 200 && statusCode < 300;
        }
    }

    private enum Scenario {
        MISTAKE_LIST("mistake-list", "/api/v1/feedback/review/list?page=0&size=10", true),
        OVERVIEW("overview", "/api/v1/overview/get_overview", true),
        KNOWLEDGE_POINTS("knowledge-points", "/api/v1/keypoints_explanation/get_key_points", true),
        LOGIN("login", "/api/userAccount/login", false);

        private final String value;
        private final String path;
        private final boolean requiresAuthentication;

        Scenario(String value, String path, boolean requiresAuthentication) {
            this.value = value;
            this.path = path;
            this.requiresAuthentication = requiresAuthentication;
        }

        static Scenario from(String value) {
            for (Scenario scenario : values()) {
                if (scenario.value.equals(value)) {
                    return scenario;
                }
            }
            throw new IllegalArgumentException("Unsupported scenario: " + value);
        }

        String path() {
            return path;
        }

        boolean requiresAuthentication() {
            return requiresAuthentication;
        }
    }

    private record Config(
            URI baseUri,
            String scenario,
            int warmupRequests,
            int requestCount,
            int concurrency,
            int timeoutSeconds,
            String account,
            String password,
            Path output) {
        static Config parse(String[] args) {
            Map<String, String> values = new HashMap<>();
            for (int index = 0; index < args.length; index += 2) {
                if (!args[index].startsWith("--") || index + 1 >= args.length) {
                    throw new IllegalArgumentException("Arguments must use --name value format.");
                }
                values.put(args[index].substring(2), args[index + 1]);
            }
            URI baseUri = URI.create(values.getOrDefault("base-url", DEFAULT_BASE_URL).replaceAll("/+$", "") + "/");
            Config config = new Config(
                    baseUri,
                    values.getOrDefault("scenario", "mistake-list"),
                    integer(values, "warmup", 100),
                    integer(values, "requests", 1_000),
                    integer(values, "concurrency", 20),
                    integer(values, "timeout-seconds", 15),
                    values.getOrDefault("account", DEFAULT_ACCOUNT),
                    values.getOrDefault("password", DEFAULT_PASSWORD),
                    values.containsKey("output") ? Path.of(values.get("output")) : null);
            if (config.warmupRequests() < 0 || config.requestCount() < 1 || config.concurrency() < 1
                    || config.timeoutSeconds() < 1) {
                throw new IllegalArgumentException("Warmup must be zero or greater. Requests, concurrency, and timeout must be positive.");
            }
            Scenario.from(config.scenario());
            return config;
        }

        private static int integer(Map<String, String> values, String key, int defaultValue) {
            return Integer.parseInt(values.getOrDefault(key, Integer.toString(defaultValue)));
        }
    }

    private record BenchmarkSummary(
            String scenario,
            String baseUrl,
            int warmupRequests,
            int requestCount,
            int concurrency,
            long elapsedMilliseconds,
            double throughputRps,
            int successfulRequests,
            int failedRequests,
            Map<String, Integer> statusCounts,
            Map<String, Integer> errorCounts,
            Latency latency) {
        static BenchmarkSummary from(Config config, Scenario scenario, List<Result> results, long elapsedNanos) {
            List<Long> elapsed = results.stream().map(Result::elapsedNanos).sorted().toList();
            Map<String, Integer> statuses = new HashMap<>();
            Map<String, Integer> errors = new HashMap<>();
            int successful = 0;
            for (Result result : results) {
                if (result.successful()) {
                    successful++;
                }
                if (result.statusCode() != null) {
                    statuses.merge(Integer.toString(result.statusCode()), 1, Integer::sum);
                }
                if (result.errorType() != null) {
                    errors.merge(result.errorType(), 1, Integer::sum);
                }
            }
            return new BenchmarkSummary(
                    scenario.value,
                    config.baseUri().toString().replaceAll("/$", ""),
                    config.warmupRequests(),
                    config.requestCount(),
                    config.concurrency(),
                    nanosToMilliseconds(elapsedNanos),
                    results.size() * 1_000_000_000D / elapsedNanos,
                    successful,
                    results.size() - successful,
                    sort(statuses),
                    sort(errors),
                    Latency.from(elapsed));
        }

        String toJson() {
            return "{" +
                    "\"timestamp\":\"" + Instant.now() + "\"," +
                    "\"scenario\":\"" + scenario + "\"," +
                    "\"baseUrl\":\"" + baseUrl + "\"," +
                    "\"warmupRequests\":" + warmupRequests + "," +
                    "\"requestCount\":" + requestCount + "," +
                    "\"concurrency\":" + concurrency + "," +
                    "\"elapsedMilliseconds\":" + elapsedMilliseconds + "," +
                    "\"throughputRps\":" + decimal(throughputRps) + "," +
                    "\"successfulRequests\":" + successfulRequests + "," +
                    "\"failedRequests\":" + failedRequests + "," +
                    "\"statusCounts\":" + mapToJson(statusCounts) + "," +
                    "\"errorCounts\":" + mapToJson(errorCounts) + "," +
                    "\"latencyMs\":" + latency.toJson() +
                    "}";
        }

        private static Map<String, Integer> sort(Map<String, Integer> source) {
            return source.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (left, right) -> left,
                            java.util.LinkedHashMap::new));
        }
    }

    private record Latency(long min, long p50, long p95, long p99, long max, double average) {
        static Latency from(List<Long> nanos) {
            if (nanos.isEmpty()) {
                return new Latency(0, 0, 0, 0, 0, 0D);
            }
            long total = nanos.stream().mapToLong(Long::longValue).sum();
            return new Latency(
                    nanosToMilliseconds(nanos.getFirst()),
                    nanosToMilliseconds(percentile(nanos, 0.50D)),
                    nanosToMilliseconds(percentile(nanos, 0.95D)),
                    nanosToMilliseconds(percentile(nanos, 0.99D)),
                    nanosToMilliseconds(nanos.getLast()),
                    total / 1_000_000D / nanos.size());
        }

        String toJson() {
            return "{" +
                    "\"min\":" + min + "," +
                    "\"p50\":" + p50 + "," +
                    "\"p95\":" + p95 + "," +
                    "\"p99\":" + p99 + "," +
                    "\"max\":" + max + "," +
                    "\"average\":" + decimal(average) +
                    "}";
        }

        private static long percentile(List<Long> values, double percentile) {
            int index = (int) Math.ceil(values.size() * percentile) - 1;
            return values.get(Math.max(0, index));
        }
    }

    private static long nanosToMilliseconds(long nanos) {
        return Math.round(nanos / 1_000_000D);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String mapToJson(Map<String, Integer> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append('\"').append(jsonEscape(entry.getKey())).append("\":").append(entry.getValue());
            first = false;
        }
        return json.append('}').toString();
    }
}
