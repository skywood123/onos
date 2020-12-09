package org.onosproject.meterconfiguration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

public class HTTPServer {
    protected final HttpServer server;

    protected final ExecutorService executorService;

    private static class LocalByteArray extends ThreadLocal<ByteArrayOutputStream> {
        private LocalByteArray() {}

        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(1048576);
        }
    }

    static class HTTPMetricHandler implements HttpHandler {
        private CollectorRegistry registry;

        private final HTTPServer.LocalByteArray response = new HTTPServer.LocalByteArray();

        private static final String HEALTHY_RESPONSE = "Exporter is Healthy.";

        HTTPMetricHandler(CollectorRegistry registry) {
            this.registry = registry;
        }

        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getRawQuery();
            String contextPath = t.getHttpContext().getPath();
            ByteArrayOutputStream response = this.response.get();
            response.reset();
            OutputStreamWriter osw = new OutputStreamWriter(response);
            if ("/-/healthy".equals(contextPath)) {
                osw.write("Exporter is Healthy.");
            } else {
                TextFormat.write004(osw, this.registry
                        .filteredMetricFamilySamples(HTTPServer.parseQuery(query)));
            }
            osw.flush();
            osw.close();
            response.flush();
            response.close();
            t.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            if (HTTPServer.shouldUseCompression(t)) {
                t.getResponseHeaders().set("Content-Encoding", "gzip");
                t.sendResponseHeaders(200, 0L);
                GZIPOutputStream os = new GZIPOutputStream(t.getResponseBody());
                response.writeTo(os);
                os.close();
            } else {
                t.getResponseHeaders().set("Content-Length",
                                           String.valueOf(response.size()));
                t.sendResponseHeaders(200, response.size());
                response.writeTo(t.getResponseBody());
            }
            t.close();
        }
    }

    protected static boolean shouldUseCompression(HttpExchange exchange) {
        List<String> encodingHeaders = exchange.getRequestHeaders().get("Accept-Encoding");
        if (encodingHeaders == null)
            return false;
        for (String encodingHeader : encodingHeaders) {
            String[] encodings = encodingHeader.split(",");
            for (String encoding : encodings) {
                if (encoding.trim().toLowerCase().equals("gzip"))
                    return true;
            }
        }
        return false;
    }

    protected static Set<String> parseQuery(String query) throws IOException {
        Set<String> names = new HashSet<String>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx != -1 && URLDecoder.decode(pair.substring(0, idx), "UTF-8").equals("name[]"))
                    names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
        }
        return names;
    }

    static class NamedDaemonThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

        private final int poolNumber = POOL_NUMBER.getAndIncrement();

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final ThreadFactory delegate;

        private final boolean daemon;

        NamedDaemonThreadFactory(ThreadFactory delegate, boolean daemon) {
            this.delegate = delegate;
            this.daemon = daemon;
        }

        public Thread newThread(Runnable r) {
            Thread t = this.delegate.newThread(r);
            t.setName(String.format("prometheus-http-%d-%d", new Object[] { Integer.valueOf(this.poolNumber), Integer.valueOf(this.threadNumber.getAndIncrement()) }));
            t.setDaemon(this.daemon);
            return t;
        }

        static ThreadFactory defaultThreadFactory(boolean daemon) {
            return new NamedDaemonThreadFactory(Executors.defaultThreadFactory(), daemon);
        }
    }

    public HTTPServer(HttpServer httpServer, CollectorRegistry registry, boolean daemon) throws IOException {
        if (httpServer.getAddress() == null)
            throw new IllegalArgumentException("HttpServer hasn't been bound to an address");
        this.server = httpServer;
        HttpHandler mHandler = new HTTPMetricHandler(registry);
        this.server.createContext("/", mHandler);
        this.server.createContext("/metrics", mHandler);
        this.server.createContext("/-/healthy", mHandler);
        this.executorService = Executors.newFixedThreadPool(5, NamedDaemonThreadFactory.defaultThreadFactory(daemon));
        this.server.setExecutor(this.executorService);
        start(daemon);
    }

    public HTTPServer(InetSocketAddress addr, CollectorRegistry registry, boolean daemon) throws IOException {
        this(HttpServer.create(addr, 3), registry, daemon);
    }

    public HTTPServer(InetSocketAddress addr, CollectorRegistry registry) throws IOException {
        this(addr, registry, false);
    }

    public HTTPServer(int port, boolean daemon) throws IOException {
        this(new InetSocketAddress(port), CollectorRegistry.defaultRegistry, daemon);
    }

    public HTTPServer(int port) throws IOException {
        this(port, false);
    }

    public HTTPServer(String host, int port, boolean daemon) throws IOException {
        this(new InetSocketAddress(host, port), CollectorRegistry.defaultRegistry, daemon);
    }

    public HTTPServer(String host, int port) throws IOException {
        this(new InetSocketAddress(host, port), CollectorRegistry.defaultRegistry, false);
    }

    private void start(boolean daemon) {
        if (daemon == Thread.currentThread().isDaemon()) {
            this.server.start();
        } else {
            FutureTask<Void> startTask = new FutureTask<Void>(new Runnable() {
                public void run() {
                    HTTPServer.this.server.start();
                }
            },  null);
            NamedDaemonThreadFactory.defaultThreadFactory(daemon).newThread(startTask).start();
            try {
                startTask.get();
            } catch (ExecutionException e) {
                throw new RuntimeException("Unexpected exception on starting HTTPSever", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        this.server.stop(0);
        this.executorService.shutdown();
    }

    public int getPort() {
        return this.server.getAddress().getPort();
    }
}
