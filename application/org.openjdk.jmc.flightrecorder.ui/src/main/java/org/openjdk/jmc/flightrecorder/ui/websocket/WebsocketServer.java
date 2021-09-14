package org.openjdk.jmc.flightrecorder.ui.websocket;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.servlet.WebSocketUpgradeFilter;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.serializers.json.IItemCollectionJsonSerializer;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;

public class WebsocketServer {

	private static int MAX_MESSAGE_SIZE = 1024 * 1024 * 1024;
	private static int IDLE_TIMEOUT_MINUTES = 5;

	private final int port;
	private List<WebSocketConnectionHandler> handlers = new ArrayList<>();
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private IItemCollection currentSelection = null;

	public WebsocketServer(int port) {
		this.port = port;
		executorService.execute(() -> startServer());
	}

	private void startServer() {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setHost("127.0.0.1");
		connector.setPort(port);
		server.addConnector(connector);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		JettyWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
			container.setMaxBinaryMessageSize(MAX_MESSAGE_SIZE);
			container.setIdleTimeout(Duration.ofMinutes(IDLE_TIMEOUT_MINUTES));
			container.addMapping("/events/*", (req, resp) -> {
				// try to send the current selection when the client connects
				// for simplicity, we serialise for every new connection
				String eventsJson = currentSelection != null
						? IItemCollectionJsonSerializer.toJsonString(currentSelection) : null;
				WebSocketConnectionHandler handler = new WebSocketConnectionHandler(eventsJson);
				handlers.add(handler);
				return handler;
			});
		});

		try {
			WebSocketUpgradeFilter.ensureFilter(context.getServletContext());
			server.start();
			server.join();
		} catch (Exception e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to start websocket server", e);
		}
	}

	public void notifyAll(IItemCollection events) {
		currentSelection = events;
		handlers = handlers.stream().filter(h -> h.isConnected()).collect(Collectors.toList());
		if (handlers.size() == 0) {
			// do nothing if no handlers are registered
			return;
		}
		String eventsJson = IItemCollectionJsonSerializer.toJsonString(events);

		handlers.forEach(handler -> handler.sendMessage(eventsJson));
	}

	private static class WebSocketConnectionHandler extends WebSocketAdapter {
		private CountDownLatch closureLatch = new CountDownLatch(1);
		private String firstMessage;

		WebSocketConnectionHandler(String firstMessage) {
			this.firstMessage = firstMessage;
		}

		public void sendMessage(String message) {
			if (getSession() != null && isConnected()) {
				FlightRecorderUI.getDefault().getLogger().log(Level.INFO,
						"Sending message to " + getSession().getRemoteAddress().toString());
				try {
					getSession().getRemote().sendString(message);
				} catch (IOException e) {
					FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to send websocket message", e);
				}
			}
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			FlightRecorderUI.getDefault().getLogger().log(Level.INFO,
					"Socket connected to " + sess.getRemoteAddress().toString());
			try {
				if (firstMessage != null) {
					getSession().getRemote().sendString(firstMessage);
					firstMessage = null;
				}
			} catch (IOException e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to show outline view", e);
			}
		}

		@Override
		public void onWebSocketText(String message) {
			super.onWebSocketText(message);
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			super.onWebSocketClose(statusCode, reason);
			FlightRecorderUI.getDefault().getLogger().log(Level.INFO, "Socket closed: [" + statusCode + "] " + reason);
			closureLatch.countDown();
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			super.onWebSocketError(cause);
			if (cause.getCause() instanceof TimeoutException) {
				FlightRecorderUI.getDefault().getLogger().log(Level.INFO, "Websocket timed out");
			} else {
				FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Websocket error", cause);
			}
		}

		@SuppressWarnings("unused")
		// TODO: graceful shutdown
		public void awaitClosure() throws InterruptedException {
			closureLatch.await();
		}
	}
}
