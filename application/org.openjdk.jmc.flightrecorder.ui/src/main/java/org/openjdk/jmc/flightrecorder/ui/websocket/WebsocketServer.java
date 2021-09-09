package org.openjdk.jmc.flightrecorder.ui.websocket;

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

public class WebsocketServer {
	private static int PORT = 8029;

	private final List<WebSocketConnectionHandler> treeModelHandlers = new ArrayList<>();
	private final List<WebSocketConnectionHandler> eventModelHandlers = new ArrayList<>();
	private String lastTreeBroadcast = null;
	private String lastEventsBroadcast = null;

	public WebsocketServer() {
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
					nativeWebSocketConfiguration.getPolicy().setMaxTextMessageBufferSize(1024 * 1024 * 1024);

					// Add websockets
					nativeWebSocketConfiguration.addMapping("/tree/*", (req, resp) -> {
						WebSocketConnectionHandler handler = new WebSocketConnectionHandler(lastTreeBroadcast);
						// FIXME: this is a memory leak, handlers are not cleared after clients disconnect
						treeModelHandlers.add(handler);
						return handler;
					});

					nativeWebSocketConfiguration.addMapping("/events/*", (req, resp) -> {
						WebSocketConnectionHandler handler = new WebSocketConnectionHandler(lastEventsBroadcast);
						// FIXME: this is a memory leak, handlers are not cleared after clients disconnect
						eventModelHandlers.add(handler);
						// try to send the last broadcast when the client connects
						if (lastEventsBroadcast != null) {
							try {
								handler.sendMessage(lastEventsBroadcast);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
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

	public void broadcastTree(String message) {
		treeModelHandlers.forEach(handler -> {
			try {
				handler.sendMessage(message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		lastTreeBroadcast = message;
	}

	public void broadcastEvents(String message) {
		eventModelHandlers.forEach(handler -> {
			try {
				handler.sendMessage(message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		lastEventsBroadcast = message;
	}

	private static class WebSocketConnectionHandler extends WebSocketAdapter {
		private CountDownLatch closureLatch = new CountDownLatch(1);
		private String lastSent;

		WebSocketConnectionHandler(String lastEventsBroadcast) {
			this.lastSent = lastEventsBroadcast;
		}

		public void sendMessage(String message) throws IOException {
			if (getSession() != null) {
				System.out.println("sending message to " + getSession().getRemoteAddress().toString());
				getSession().getRemote().sendString(message);
				lastSent = message;
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
				if (lastSent != null) {
					getSession().getRemote().sendString(lastSent);
				}
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
