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
package org.openjdk.jmc.ui.test.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

import org.openjdk.jmc.ui.misc.ProgressCircle;

/**
 * Simple test driver for the ProgressCircle Swing component.
 */
public class ProgressCircleTester {
	private static final int REPAINT_PERIOD = 200;
	private static final long ROTATION_TIME = 5000; // 5s for one rotation

	public static void main(String[] args) throws Exception {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final ProgressCircle c = new ProgressCircle();
		c.setForeground(new Color(128, 0, 128));
		c.setOpaque(true);
		f.setSize(400, 400);
		f.getContentPane().add(c);
		final JLabel status = new JLabel();
		status.setForeground(Color.GRAY);
		f.getContentPane().add(status, BorderLayout.SOUTH);
		status.setText(getStatusText(c));
		f.setVisible(true);
		f.getContentPane().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				c.setAntialiasing(!c.isAntialiasing());
				status.setText(getStatusText(c));
			}
		});
		final long startTime = System.currentTimeMillis();
		Timer t = new Timer(REPAINT_PERIOD, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				long time = System.currentTimeMillis();
				int alpha = (int) Math.round((360.0d * (time - startTime) / ROTATION_TIME));
				c.setAngle(alpha);
				c.paintImmediately(c.getBounds());
			}
		});
		t.start();
	}

	private static String getStatusText(ProgressCircle c) {
		return String.format("Antialiasing is %s. Press mouse to change!", c.isAntialiasing() ? "on" : "off");
	}
}
