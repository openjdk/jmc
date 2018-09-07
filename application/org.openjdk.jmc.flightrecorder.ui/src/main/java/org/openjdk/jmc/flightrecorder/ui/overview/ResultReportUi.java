/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.ui.overview;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.HtmlResultGroup;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.HtmlResultProvider;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.RulesHtmlToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.ui.DataPageDescriptor;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * This class handles creation of a HTML/JS based result report, it has two modes, Single Page and
 * Full Report. Single Page is used by the ResultPage PageBookView and the Full Report by the Result
 * Overview page.
 */
public class ResultReportUi {

	private static final String OVERVIEW_MAKE_SCALABLE = "overview.makeScalable();"; //$NON-NLS-1$
	private static final String OVERVIEW_UPDATE_PAGE_HEADERS_VISIBILITY = "overview.updatePageHeadersVisibility();"; //$NON-NLS-1$
	private static final Pattern HTML_ANCHOR_PATTERN = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
	private static final String OPEN_BROWSER_WINDOW = "openWindowByUrl";

	private static class Linker extends BrowserFunction {

		private Iterable<HtmlResultGroup> resultGroups;
		private IPageContainer editor;

		public Linker(Browser browser, String name, Iterable<HtmlResultGroup> resultGroups, IPageContainer editor) {
			super(browser, name);
			this.resultGroups = resultGroups;
			this.editor = editor;
		}

		@Override
		public Object function(Object[] arguments) {
			if (arguments.length != 1 && !(arguments[0] instanceof String)) {
				return null;
			}
			String id = arguments[0].toString();
			for (HtmlResultGroup group : resultGroups) {
				if (group instanceof PageDescriptorResultGroup && id.equals(group.getId())) {
					editor.navigateTo(((PageDescriptorResultGroup) group).getDescriptor());
					return null;
				} else {
					if (hasPageAsChild(group, id)) {
						return null;
					}
				}
			}
			return null;
		}

		private boolean hasPageAsChild(HtmlResultGroup parent, String id) {
			if (parent instanceof PageDescriptorResultGroup && id.equals(parent.getId())) {
				editor.navigateTo(((PageDescriptorResultGroup) parent).getDescriptor());
				return true;
			}
			if (!parent.hasChildren()) {
				return false;
			} else {
				for (HtmlResultGroup child : parent.getChildren()) {
					if (hasPageAsChild(child, id)) {
						return true;
					}
				}
			}
			return false;
		}

	}

	private class Expander extends BrowserFunction {

		public Expander(Browser browser, String name) {
			super(browser, name);
		}

		@Override
		public Object function(Object[] arguments) {
			resultExpandedStates.put(arguments[0].toString(), (Boolean) arguments[1]);
			return null;
		}

	}

	public class OpenWindowFunction extends BrowserFunction {

		public OpenWindowFunction (final Browser browser, final String name) {
		    super(browser, name);
		}
		public Object function (Object[] arguments) {
			final String url = String.valueOf(arguments[0]);
		    final String title = String.valueOf(arguments[1]);
		    openBrowserByUrl(url, title);
		    return null;
		}
	}

	private static class PageContainerResultProvider implements HtmlResultProvider {
		private IPageContainer editor;

		public PageContainerResultProvider(IPageContainer editor) {
			this.editor = editor;
		}

		@Override
		public Collection<Result> getResults(Collection<String> topics) {
			return editor.getRuleManager().getResults(topics);
		}
	}

	private static class PageDescriptorResultGroup implements HtmlResultGroup {
		private DataPageDescriptor descriptor;
		private List<HtmlResultGroup> children;

		public PageDescriptorResultGroup(DataPageDescriptor descriptor) {
			this.descriptor = descriptor;
			children = new ArrayList<>();
			for (DataPageDescriptor dpdChild : descriptor.getChildren()) {
				children.add(new PageDescriptorResultGroup(dpdChild));
			}
		}

		@Override
		public List<HtmlResultGroup> getChildren() {
			return children;
		}

		@Override
		public boolean hasChildren() {
			return !children.isEmpty();
		}

		@Override
		public Collection<String> getTopics() {
			return Stream.of(descriptor.getTopics()).collect(Collectors.toList());
		}

		@Override
		public String getId() {
			return Integer.toString(descriptor.hashCode());
		}

		@Override
		public String getName() {
			return descriptor.getName();
		}

		@Override
		public String getImage() {
			ImageDescriptor image = descriptor.getImageDescriptor();
			if (image == null) {
				return null;
			}
			ImageLoader loader = new ImageLoader();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			loader.data = new ImageData[] {image.getImageData()};
			loader.save(out, SWT.IMAGE_PNG);
			return Base64.getEncoder().encodeToString(out.toByteArray());
		}

		public DataPageDescriptor getDescriptor() {
			return descriptor;
		}

		public static Collection<HtmlResultGroup> build(Collection<DataPageDescriptor> descriptors) {
			return descriptors.stream().map(dpd -> new PageDescriptorResultGroup(dpd)).collect(Collectors.toList());
		}
	}

	private final HashMap<String, Boolean> resultExpandedStates = new HashMap<>();
	private Boolean showOk = false;
	private Boolean isLoaded = false;

	private Browser browser;
	private IPageContainer editor;
	private Collection<HtmlResultGroup> descriptors;
	private boolean isSinglePage = false;

	private void openBrowserByUrl(final String url, final String title) {
		final Display display = Display.getDefault();
		final Shell shell = new Shell(display);
		shell.setText(title);
		shell.setLayout(new FillLayout());
		final Browser browser = new Browser(shell, SWT.NONE);
		initializeBrowser(display, browser, shell);
		shell.open();
		browser.setUrl(url);
	}

	private void initializeBrowser(final Display display, final Browser browser, final Shell shell) {
		browser.addOpenWindowListener(new OpenWindowListener() {
			public void open(WindowEvent event) {
				  initializeBrowser(display, browser, shell);
				  event.browser = browser;
			    }
		});
		browser.addCloseWindowListener(new CloseWindowListener() {
			  public void close(WindowEvent event) {
				  Browser browser = (Browser)event.widget;
			      Shell shell = browser.getShell();
			      shell.close();
			  }
	    });
	}

	/*
     * We replace the anchors in the HTML when running in the JMC UI to make
     * it possible to follow them. See JMC-5419 for more information.
     */
	private static String adjustAnchorFollowAction(String html) {
		Map<String, String> map = new HashMap<>();
		Matcher m = HTML_ANCHOR_PATTERN.matcher(html);
		while (m.find()) {
			map.put(m.group(1), m.group(2));
		}
		for(Map.Entry<String, String> e: map.entrySet()){
			html = html.replace(e.getKey(), openWindowMethod(e.getKey(), e.getValue()));
		}
		return html;
	}

	private static String openWindowMethod(String url, String name){
        return new StringBuilder().append("#\" onclick=\"").append(OPEN_BROWSER_WINDOW).append("(").append("\u0027")
                .append(url).append("\u0027").append(',').append("\u0027")
                .append(name).append("\u0027").append(")").toString();
    }

	public ResultReportUi(boolean isSinglePage) {
		this.isSinglePage = isSinglePage;
	}

	public List<String> getHtml(IPageContainer editor) {
		List<String> overviewHtml = new ArrayList<>(1);
		String adjustedHtml = adjustAnchorFollowAction(RulesHtmlToolkit.generateStructuredHtml(new PageContainerResultProvider(editor), descriptors,
				resultExpandedStates, true));
		overviewHtml.add(adjustedHtml);
		return overviewHtml;
	}

	public void setShowOk(boolean showOk) {
		this.showOk = showOk;
		if (!isSinglePage) {
			try {
				// FIXME: Avoid implicit dependency on HTML/javascript template. Generate script in RulesHtmlToolkit instead
				browser.evaluate(String.format("overview.showOk(%b);", showOk)); //$NON-NLS-1$
				boolean allOk = editor.getRuleManager().getScoreStream(topics.toArray(new String[topics.size()]))
						.allMatch(d -> d > RulesHtmlToolkit.IN_PROGRESS && d < Severity.INFO.getLimit()) && !showOk;
				browser.evaluate(String.format("overview.allOk(%b);", allOk)); //$NON-NLS-1$
			} catch (SWTException swte) {
				String html = RulesHtmlToolkit.generateStructuredHtml(new PageContainerResultProvider(editor),
						descriptors, resultExpandedStates, false);
				String adjustedHtml = adjustAnchorFollowAction(html);
				browser.setText(adjustedHtml);
			}
		}
	}

	boolean getShowOk() {
		return showOk;
	}

	private ConcurrentLinkedQueue<IRule> resultEventQueue = new ConcurrentLinkedQueue<>();
	private Collection<String> topics = RulesToolkit.getAllTopics();
	private Collection<Result> results;

	public void updateRule(IRule rule) {
		// FIXME: Possible race condition where elements may be added to the queue without being consumed
		if (resultEventQueue.isEmpty()) {
			resultEventQueue.add(rule);
			DisplayToolkit.safeAsyncExec(() -> {
				if (browser.isDisposed()) {
					return;
				}
				// FIXME: Avoid implicit dependency on HTML/javascript template. Generate script in RulesHtmlToolkit instead
				StringBuilder script = new StringBuilder();
				while (!resultEventQueue.isEmpty()) {
					IRule next = resultEventQueue.poll();
					Result result = editor.getRuleManager().getResult(next);
					if (result == null) {
						continue;
					}
					long score = Math.round(result.getScore());
					String adjustedHtml = adjustAnchorFollowAction(RulesHtmlToolkit.getDescription(result));//$NON-NLS-1$ //$NON-NLS-2$
					String quoteEscape = adjustedHtml.replaceAll("\\\"", "\\\\\""); //$NON-NLS-1$ //$NON-NLS-2$
					String description = quoteEscape.replaceAll("\n", "</br>"); //$NON-NLS-1$ //$NON-NLS-2$
					script.append(String.format("overview.updateResult(\"%s\", %d, \"%s\");", //$NON-NLS-1$
							result.getRule().getId(), score, description));
				}
				String[] topicsArray = topics.toArray(new String[topics.size()]);
				if (!isSinglePage) {
					boolean allOk = editor.getRuleManager().getScoreStream(topicsArray)
							.allMatch(d -> d > RulesHtmlToolkit.IN_PROGRESS && d < Severity.INFO.getLimit()) && !showOk;
					script.append(String.format("overview.allOk(%b);", allOk)); //$NON-NLS-1$
				}
				boolean allIgnored = editor.getRuleManager().getScoreStream(topicsArray)
						.allMatch(d -> d == Result.IGNORE);
				script.append(String.format("overview.allIgnored(%b);", allIgnored)); //$NON-NLS-1$
				try {
					if (isLoaded) {
						browser.evaluate(script.toString());
						browser.evaluate(OVERVIEW_UPDATE_PAGE_HEADERS_VISIBILITY);
					} else {
						final String finalScript = script.toString();
						browser.addProgressListener(new ProgressAdapter() {
							@Override
							public void completed(ProgressEvent event) {
								browser.evaluate(finalScript);
								isLoaded = true;
								browser.evaluate(OVERVIEW_UPDATE_PAGE_HEADERS_VISIBILITY);
								browser.removeProgressListener(this);
							}
						});
					}
				} catch (IllegalArgumentException | SWTException e) {
					try {
						FlightRecorderUI.getDefault().getLogger().log(Level.INFO,
								"Could not update single result, redrawing html view. " + e.getMessage()); //$NON-NLS-1$
						String html = isSinglePage ? RulesHtmlToolkit.generateSinglePageHtml(results)
								: RulesHtmlToolkit.generateStructuredHtml(new PageContainerResultProvider(editor),
										descriptors, resultExpandedStates, false);
						String adjustedHtml = adjustAnchorFollowAction(html);
						browser.setText(adjustedHtml);
					} catch (Exception e1) {
						FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Could not update Result Overview", //$NON-NLS-1$
								e1);
					}
				}
			});
		} else {
			resultEventQueue.add(rule);
		}
	}

	public void setResults(Collection<Result> results) {
		this.results = results;
	}

	public boolean createHtmlOverview(Browser browser, IPageContainer editor, IState state) {
		this.browser = browser;
		this.editor = editor;
		descriptors = PageDescriptorResultGroup.build(FlightRecorderUI.getDefault().getPageManager().getRootPages());
		try {
			this.showOk = Boolean.valueOf(state.getChild("report").getChild("showOk").getAttribute("value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (NullPointerException npe) {
		}
		browser.addListener(SWT.MenuDetect, new Listener() {
			@Override
			public void handleEvent(Event event) {
				event.doit = false;
			}
		});
		try {
			String html = isSinglePage ? RulesHtmlToolkit.generateSinglePageHtml(results)
					: RulesHtmlToolkit.generateStructuredHtml(new PageContainerResultProvider(editor), descriptors,
							resultExpandedStates, false);
			String adjustedHtml = adjustAnchorFollowAction(html);
			browser.setText(adjustedHtml, true);
			browser.setJavascriptEnabled(true);
			browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(ProgressEvent event) {
					new OpenWindowFunction(browser, OPEN_BROWSER_WINDOW); //$NON-NLS-1$
					new Linker(browser, "linker", descriptors, editor); //$NON-NLS-1$
					new Expander(browser, "expander"); //$NON-NLS-1$
					browser.execute(String.format("overview.showOk(%b);", showOk)); //$NON-NLS-1$
					if (isSinglePage) {
						browser.execute(OVERVIEW_MAKE_SCALABLE);
					}
					browser.evaluate(OVERVIEW_UPDATE_PAGE_HEADERS_VISIBILITY);
					isLoaded = true;
				}
			});
		} catch (IOException | IllegalArgumentException e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Could not create Report Overview", e); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	public void saveTo(IWritableState state) {
		state.createChild("report").createChild("showOk").putString("value", showOk.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
