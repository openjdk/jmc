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
package org.openjdk.jmc.rcp.application;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;

import org.openjdk.jmc.rcp.application.actions.OpenFileAction;
import org.openjdk.jmc.rcp.application.scripting.ShellViewCoommand;
import org.openjdk.jmc.ui.common.util.Environment;
import org.openjdk.jmc.ui.common.util.Environment.OSType;
import org.openjdk.jmc.ui.handlers.ExternalUrlAction;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
	private static final String FORUM_URL = "https://community.oracle.com/community/java/java_hotspot_virtual_machine/java_mission_control"; //$NON-NLS-1$
	private static final String OTN_URL = "http://www.oracle.com/missioncontrol"; //$NON-NLS-1$

	private IWorkbenchAction exitAction;
	private IWorkbenchAction preferenceAction;
	private IWorkbenchAction aboutAction;
	private IWorkbenchAction closeAction;
	private IWorkbenchAction closeAllAction;
	private IWorkbenchAction saveAction;
	private IWorkbenchAction saveAsAction;
	private IWorkbenchAction introAction;
	private IWorkbenchAction helpAction;
	private IWorkbenchAction helpSearchAction;
	private IWorkbenchAction dynamicHelpAction;
	private OpenFileAction openAction;
	private IWorkbenchAction backwardAction;
	private IWorkbenchAction forwardAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	protected void makeActions(final IWorkbenchWindow window) {
		openAction = new OpenFileAction(window);
		openAction.setActionDefinitionId("org.openjdk.jmc.rcp.application.file.open"); //$NON-NLS-1$

		saveAction = ActionFactory.SAVE.create(window);
		saveAsAction = ActionFactory.SAVE_AS.create(window);

		closeAction = ActionFactory.CLOSE.create(window);
		register(closeAction);

		closeAllAction = ActionFactory.CLOSE_ALL.create(window);
		register(closeAction);

		if (!(Environment.getOSType() == OSType.MAC)) {
			preferenceAction = ActionFactory.PREFERENCES.create(window);
			register(preferenceAction);
		}
		aboutAction = ActionFactory.ABOUT.create(window);
		register(aboutAction);
		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);
		if (Platform.getProduct() != null) {
			introAction = ActionFactory.INTRO.create(window);
			register(introAction);
		}
		register(new ShellViewCoommand());
		helpAction = ActionFactory.HELP_CONTENTS.create(window);
		helpAction.setText(Messages.ApplicationActionBarAdvisor_MENU_ITEM_HELP_TEXT);
		helpSearchAction = ActionFactory.HELP_SEARCH.create(window);
		register(helpSearchAction);
		dynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(window);
		register(dynamicHelpAction);

		backwardAction = ActionFactory.BACKWARD_HISTORY.create(window);
		backwardAction.setId(IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY);
		register(backwardAction);
		forwardAction = ActionFactory.FORWARD_HISTORY.create(window);
		forwardAction.setId(IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY);
		register(forwardAction);
	}

	private void createDebug(IMenuManager manager) {
		MenuManager menu = new MenuManager(Messages.ApplicationActionBarAdvisor_MENU_DEBUG_TEXT, "mcdebug"); //$NON-NLS-1$
		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.setVisible(Environment.isDebug());
		manager.add(menu);
	}

	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		IWorkbenchWindow window = getActionBarConfigurer().getWindowConfigurer().getWindow();
		menuBar.add(createFileMenu(window));
		menuBar.add(createEditMenu(window));
		menuBar.add(createNavigateMenu(window));
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(createWindowMenu(window));
		menuBar.add(createHelpMenu(window));

	}

	private MenuManager createHelpMenu(IWorkbenchWindow window) {
		MenuManager helpMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_MENU_HELP_TEXT,
				IWorkbenchActionConstants.M_HELP);

		if (introAction != null) {
			helpMenu.add(introAction);
			helpMenu.add(new Separator());
		}

		helpMenu.add(helpAction);
		helpMenu.add(helpSearchAction);
		helpMenu.add(dynamicHelpAction);
		helpMenu.add(new Separator());

		helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
		helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
		helpMenu.add(new Separator());

		helpMenu.add(new ExternalUrlAction(OTN_URL, Messages.ApplicationActionBarAdvisor_MENU_ITEM_OTN_TEXT));
		helpMenu.add(new ExternalUrlAction(FORUM_URL, Messages.ApplicationActionBarAdvisor_MENU_ITEM_FORUM_TEXT));
		helpMenu.add(new Separator());

		helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		helpMenu.add(new Separator());

		helpMenu.add(aboutAction);
		return helpMenu;
	}

	private MenuManager createWindowMenu(IWorkbenchWindow window) {
		MenuManager windowMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_MENU_WINDOW_TEXT,
				IWorkbenchActionConstants.M_WINDOW);

		windowMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		windowMenu.add(new GroupMarker("mcadditions")); //$NON-NLS-1$
		windowMenu.add(new Separator());
		windowMenu.add(createViewMenu(window));
		windowMenu.add(createPerspectiveMenu(window));
		windowMenu.add(new Separator());
		windowMenu.add(ActionFactory.RESET_PERSPECTIVE.create(window));
		if (!(Environment.getOSType() == OSType.MAC)) {
			windowMenu.add(new Separator());
			windowMenu.add(preferenceAction);
		}

		return windowMenu;
	}

	private MenuManager createEditMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager(Messages.ApplicationActionBarAdvisor_MENU_EDIT_TEXT,
				IWorkbenchActionConstants.M_EDIT);
		menu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));
		menu.add(createItem(window, IWorkbenchCommandConstants.EDIT_CUT));
		menu.add(createItem(window, IWorkbenchCommandConstants.EDIT_COPY));
		menu.add(createItem(window, IWorkbenchCommandConstants.EDIT_PASTE));
		menu.add(new Separator());
		menu.add(createItem(window, IWorkbenchCommandConstants.EDIT_DELETE));
		menu.add(createItem(window, IWorkbenchCommandConstants.EDIT_SELECT_ALL));
		menu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
		return menu;
	}

	private static IContributionItem createItem(IServiceLocator sl, String commandId) {
		return new CommandContributionItem(
				new CommandContributionItemParameter(sl, commandId, commandId, CommandContributionItem.STYLE_PUSH));
	}

	private IContributionItem createPerspectiveMenu(IWorkbenchWindow window) {
		MenuManager perspectiveMenuMgr = new MenuManager(
				Messages.ApplicationActionBarAdvisor_SHOW_PERSPECTIVE_MENU_TEXT, "showPerspective"); //$NON-NLS-1$
		IContributionItem perspectiveMenu = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(window);
		perspectiveMenuMgr.add(perspectiveMenu);

		return perspectiveMenuMgr;
	}

	private MenuManager createViewMenu(IWorkbenchWindow window) {
		MenuManager showViewMenuMgr = new MenuManager(Messages.ApplicationActionBarAdvisor_SHOW_VIEW_MENU_TEXT,
				"showView"); //$NON-NLS-1$
		IContributionItem showViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
		showViewMenuMgr.add(showViewMenu);

		return showViewMenuMgr;
	}

	private MenuManager createFileMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager(Messages.ApplicationActionBarAdvisor_MENU_FILE_TEXT,
				IWorkbenchActionConstants.M_FILE);
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));

		// create the New submenu, using the same id for it as the New action
		String newText = Messages.ApplicationActionBarAdvisor_MENU_NEW_TEXT;
		String newId = ActionFactory.NEW.getId();
		MenuManager newMenu = new MenuManager(newText, newId);
		newMenu.add(new Separator(newId));
		newMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(newMenu);
		menu.add(openAction);
		menu.add(saveAction);
		menu.add(saveAsAction);
		createDebug(menu);

		menu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
		menu.add(new Separator());
		menu.add(closeAction);
		menu.add(closeAllAction);
		menu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
		menu.add(new Separator());
		menu.add(ActionFactory.EXPORT.create(window));
		menu.add(ActionFactory.IMPORT.create(window));
		menu.add(new GroupMarker(IWorkbenchActionConstants.IMPORT_EXT));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		// these three lines create the list of "recent" files
		menu.add(ContributionItemFactory.REOPEN_EDITORS.create(window));
		menu.add(new GroupMarker(IWorkbenchActionConstants.MRU));
		menu.add(new Separator());

		menu.add(getAction(ActionFactory.QUIT.getId()));
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
		return menu;
	}

	private MenuManager createNavigateMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager(Messages.ApplicationActionBarAdvisor_MENU_NAVIGATE_TEXT,
				IWorkbenchActionConstants.M_NAVIGATE);
		menu.add(backwardAction);
		menu.add(forwardAction);
		return menu;
	}
}
