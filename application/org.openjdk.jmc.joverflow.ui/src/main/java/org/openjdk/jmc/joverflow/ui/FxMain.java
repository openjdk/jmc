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
package org.openjdk.jmc.joverflow.ui;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.ui.model.ModelLoader;
import org.openjdk.jmc.joverflow.ui.model.ModelLoaderListener;
import org.openjdk.jmc.joverflow.ui.model.ReferenceChain;
import org.openjdk.jmc.joverflow.ui.viewers.JavaThingViewer;

/**
 * Main class for running JOverflow standalone
 */
public class FxMain extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		if (getParameters().getUnnamed().size() != 1) {
			System.out.println("Please specify hprof file argument");
			System.exit(1);
		}
		File f = new File(getParameters().getUnnamed().get(0));
		if (!f.isFile()) {
			System.out.println(f + " does not exist");
			System.exit(2);
		}

		final LoadingUi loderUi = new LoadingUi();
		final JOverflowFxUi ui = new JOverflowFxUi();
		loderUi.getChildren().add(0, ui);
		loderUi.getStylesheets().add(JOverflowFxUi.class.getResource("grey_round_tables.css").toExternalForm());
		ModelLoader loader = new ModelLoader(f.getAbsolutePath(), new ModelLoaderListener() {

			@Override
			public void onProgressUpdate(final double progress) {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						loderUi.setProgress(progress);
					}
				});
			}

			@Override
			public void onModelLoaded(final Snapshot snapshot, final Collection<ReferenceChain> model) {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						JavaThingViewer javaThingViewer = new JavaThingViewer() {

							@Override
							protected JavaHeapObject getObjectAtPostion(int globalObjectPos) {
								return snapshot.getObjectAtGlobalIndex(globalObjectPos);
							}

						};
						javaThingViewer.getUi().getStylesheets().add(getClass().getResource("grey.css").toExternalForm());
						ui.addModelListener(javaThingViewer);
						Stage instanceStage = new Stage();
						instanceStage.setScene(new Scene(javaThingViewer.getUi()));
						instanceStage.centerOnScreen();
						instanceStage.show();
						loderUi.clear();
						ui.setModel(model);
					}
				});
			}

			@Override
			public void onModelLoadFailed(Throwable failure) {
				failure.printStackTrace();
				System.err.println(failure);
				System.exit(-1);
			}
		});
		Scene scene = new Scene(loderUi);

		primaryStage.setScene(scene);
		primaryStage.show();
		Executors.newSingleThreadExecutor().submit(loader);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
