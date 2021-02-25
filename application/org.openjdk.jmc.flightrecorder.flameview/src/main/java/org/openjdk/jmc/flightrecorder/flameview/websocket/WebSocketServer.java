package org.openjdk.jmc.flightrecorder.flameview.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;

public class WebSocketServer {
	private static int PORT = 8029;

	private final List<WebSocketConnectionHandler> handlers = new ArrayList<>();

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
					// set idle timeout

					// Configure default max size
					nativeWebSocketConfiguration.getPolicy().setMaxTextMessageBufferSize(65535);

					// Add websockets
					nativeWebSocketConfiguration.addMapping("/events/*", (req, resp) -> {
						WebSocketConnectionHandler handler = new WebSocketConnectionHandler();
						// FIXME: this is a memory leak, handlers are not cleared after clients disconnect
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
			t.printStackTrace(System.err);
		}
	}

	public void broadcast(String message) {
		handlers.forEach(handler -> {
			try {
				handler.sendMessage(message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	private class WebSocketConnectionHandler extends WebSocketAdapter {
		private CountDownLatch closureLatch = new CountDownLatch(1);

		public void sendMessage(String message) throws IOException {
			if (getSession() != null) {
				getSession().getRemote().sendString(message);
			} else {
				System.out.println("session no longer available");
			}
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			System.out.println("Socket Connected: " + sess);
			// TODO: handle multiple connections
			try {
				getSession().getRemote().sendString("welcome!");
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
		}

		@Override
		public void onWebSocketText(String message) {
			super.onWebSocketText(message);
			System.out.println("Received TEXT message: " + message);

			if (message.toLowerCase(Locale.US).contains("bye")) {
				getSession().close(StatusCode.NORMAL, "Thanks");
			}
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
			cause.printStackTrace(System.err);
		}

		@SuppressWarnings("unused")
		// TODO: graceful shutdown
		public void awaitClosure() throws InterruptedException {
			System.out.println("Awaiting closure from remote");
			closureLatch.await();
		}
	}
}
