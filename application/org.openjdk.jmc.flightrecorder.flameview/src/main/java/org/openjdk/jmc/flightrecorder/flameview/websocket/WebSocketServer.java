package org.openjdk.jmc.flightrecorder.flameview.websocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;

public class WebSocketServer {
	private static int PORT = 8029;

	public WebSocketServer() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> startServer());
	}

	private void startServer() {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(PORT);
		server.addConnector(connector);

		// Setup the basic application "context" for this application at "/"
		// This is also known as the handler tree (in jetty speak)
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		// Configure specific websocket behaviour
		NativeWebSocketServletContainerInitializer.configure(context,
				(servletContext, nativeWebSocketConfiguration) -> {
					// Configure default max size
					nativeWebSocketConfiguration.getPolicy().setMaxTextMessageBufferSize(65535);

					// Add websockets
					nativeWebSocketConfiguration.addMapping("/events/*", WebSocketEndpoint.class);
				});

		// Add generic filter that will accept WebSocket upgrade.

		try {
			WebSocketUpgradeFilter.configure(context);
			server.start();
			server.join();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}
