package org.openjdk.jmc.flightrecorder.ui.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.serializers.json.IItemCollectionJsonSerializer;

public class WebsocketServer {
	private static int PORT = 8029;
	private static int MAX_MESSAGE_SIZE = 1024 * 1024 * 1024;

	private List<WebSocketConnectionHandler> handlers = new ArrayList<>();
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private IItemCollection currentSelection = null;

	public WebsocketServer() {
		executorService.execute(() -> startServer());
	}

	private void startServer() {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(PORT);
		server.addConnector(connector);

		// Setup the basic application context at "/"
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		// Configure specific websocket behaviour
		NativeWebSocketServletContainerInitializer.configure(context, (servletContext, configuration) -> {
			// set idle timeout

			configuration.getPolicy().setMaxTextMessageBufferSize(MAX_MESSAGE_SIZE);

			configuration.addMapping("/events/*", (req, resp) -> {
				System.out.println("Creating connection handler");
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
			// Add generic filter that will accept WebSocket upgrade.
			WebSocketUpgradeFilter.configure(context);
			server.start();
			server.join();
		} catch (Throwable t) {
			// TODO log error
			t.printStackTrace(System.err);
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
				System.out.println("sending message to " + getSession().getRemoteAddress().toString());
				try {
					getSession().getRemote().sendString(message);
				} catch (IOException e) {
					// TODO Log error
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			System.out.println("Socket Connected: " + sess);
			try {
				if (firstMessage != null) {
					getSession().getRemote().sendString(firstMessage);
					firstMessage = null;
				}
			} catch (IOException ex) {
				// TODO: log error
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
		}

		@Override
		public void onWebSocketText(String message) {
			super.onWebSocketText(message);
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			super.onWebSocketClose(statusCode, reason);
			System.out.println("Socket Closed: [" + statusCode + "] " + reason);
			closureLatch.countDown();
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			super.onWebSocketError(cause);
			if (cause.getCause() instanceof TimeoutException) {
				System.out.println("Socket timeout");
			} else {
				// TODO: log error
				cause.printStackTrace(System.err);
			}
		}

		@SuppressWarnings("unused")
		// TODO: graceful shutdown
		public void awaitClosure() throws InterruptedException {
			System.out.println("Awaiting closure from remote");
			closureLatch.await();
		}
	}
}
