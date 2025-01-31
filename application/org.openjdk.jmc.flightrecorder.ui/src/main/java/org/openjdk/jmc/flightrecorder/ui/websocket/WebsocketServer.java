/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.flightrecorder.ui.websocket;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee9.servlet.ServletContextHandler;
import org.eclipse.jetty.ee9.websocket.api.Session;
import org.eclipse.jetty.ee9.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.ee9.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee9.websocket.servlet.WebSocketUpgradeFilter;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.serializers.dot.DotSerializer;
import org.openjdk.jmc.flightrecorder.serializers.json.FlameGraphJsonSerializer;
import org.openjdk.jmc.flightrecorder.serializers.json.IItemCollectionJsonSerializer;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.StacktraceGraphModel;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;

public class WebsocketServer {

	private static int MAX_MESSAGE_SIZE = 1024 * 1024 * 1024;
	private static int IDLE_TIMEOUT_MINUTES = 5;

	private final int port;
	private Server server;
	private List<WebsocketConnectionHandler> handlers = new CopyOnWriteArrayList<>();
	private List<WebsocketConnectionHandler> treeHandlers = new CopyOnWriteArrayList<>();
	private List<WebsocketConnectionHandler> graphHandlers = new CopyOnWriteArrayList<>();
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private IItemCollection currentSelection = null;

	public WebsocketServer(int port) {
		this.port = port;
		executorService.execute(() -> startServer());
	}

	public int getPort() {
		return port;
	}

	private void startServer() {
		server = new Server();
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
				String eventsJson = WebsocketServer.toEventsJsonString(currentSelection);
				WebsocketConnectionHandler handler = new WebsocketConnectionHandler(eventsJson);
				handlers.add(handler);
				return handler;
			});
			container.addMapping("/tree/*", (req, resp) -> {
				String treeJson = WebsocketServer.toTreeModelJsonString(currentSelection);
				WebsocketConnectionHandler handler = new WebsocketConnectionHandler(treeJson);
				treeHandlers.add(handler);
				return handler;
			});
			container.addMapping("/graph/*", (req, resp) -> {
				String dot = WebsocketServer.toGraphModelDotString(currentSelection);
				WebsocketConnectionHandler handler = new WebsocketConnectionHandler(dot);
				graphHandlers.add(handler);
				return handler;
			});
		});

		try {
			WebSocketUpgradeFilter.ensureFilter(context.getServletContext());
			FlightRecorderUI.getDefault().getLogger().log(Level.INFO,
					"Starting websocket server listening on port " + port);
			server.start();
			server.join();
		} catch (Exception e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to start websocket server", e);
		}
	}

	public void notifyAll(IItemCollection events) {
		currentSelection = events;
		notifyAllEventHandlers(events);
		notifyAllGraphHandlers(events);
		notifyAllTreeHandlers(events);
	}

	private void notifyAllEventHandlers(IItemCollection events) {
		handlers = notifyAllHandlers(events, handlers, WebsocketServer::toEventsJsonString);
	}

	private void notifyAllGraphHandlers(IItemCollection events) {
		graphHandlers = notifyAllHandlers(events, graphHandlers, WebsocketServer::toGraphModelDotString);
	}

	private void notifyAllTreeHandlers(IItemCollection events) {
		treeHandlers = notifyAllHandlers(events, treeHandlers, WebsocketServer::toTreeModelJsonString);
	}

	private static String toEventsJsonString(IItemCollection items) {
		if (items == null) {
			return null;
		}
		return IItemCollectionJsonSerializer.toJsonString(items);
	}

	private static String toGraphModelDotString(IItemCollection items) {
		if (items == null) {
			return null;
		}
		FrameSeparator frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
		StacktraceGraphModel model = new StacktraceGraphModel(frameSeparator, items, null);
		return DotSerializer.toDot(model, 10_000, new HashMap<>());
	}

	private static String toTreeModelJsonString(IItemCollection items) {
		if (items == null) {
			return null;
		}
		StacktraceTreeModel model = new StacktraceTreeModel(items);
		return FlameGraphJsonSerializer.toJson(model);
	}

	private List<WebsocketConnectionHandler> notifyAllHandlers(
		IItemCollection events, List<WebsocketConnectionHandler> handlers,
		Function<IItemCollection, String> jsonSerializer) {
		handlers = handlers.stream().filter(h -> h.isConnected()).collect(Collectors.toList());
		if (handlers.size() == 0 || events == null) {
			// do nothing if no handlers are registered
			return handlers;
		}
		String json = jsonSerializer.apply(events);
		handlers.forEach(handler -> handler.sendMessage(json));
		return handlers;
	}

	public void shutdown() {
		try {
			FlightRecorderUI.getDefault().getLogger().log(Level.INFO,
					"Stopping websocket server listening on port " + port);
			server.stop();
			// TODO: see if we need to cleanup executor service and thread
		} catch (Exception e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to stop websocket server", e);
		}
	}

	private static class WebsocketConnectionHandler extends WebSocketAdapter {
		private String firstMessage;

		WebsocketConnectionHandler(String firstMessage) {
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
	}
}
