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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;

/**
 * Class that holds all the available JFR event configurations.
 */
public class EventConfigurationRepository extends Observable {
	/**
	 * Pattern to match "name (4711)" with capture groups for the name and the count.
	 */
	private static Pattern NAME_WITH_COUNT_PATTERN = Pattern.compile("^(.*)\\s*\\((\\d+)\\)$"); //$NON-NLS-1$
	/**
	 * Pattern to match " (4711)" with a capture group for the count. Ignores leading whitespace.
	 */
	private static Pattern COUNT_SUFFIX_PATTERN = Pattern.compile("^\\s*\\((\\d+)\\)$"); //$NON-NLS-1$

	// NOTE: We could sort into buckets depending on storage location too.
	private static Comparator<IEventConfiguration> COMPARATOR = new Comparator<IEventConfiguration>() {
		@Override
		public int compare(IEventConfiguration first, IEventConfiguration second) {
			return first.getName().compareTo(second.getName());
		}
	};

	private final List<IEventConfiguration> m_templates = new ArrayList<>();

	private IEventConfiguration prototype = null;

	public void remove(IEventConfiguration t) {
		if (m_templates.remove(t)) {
			setChanged();
		}
	}

	public void add(IEventConfiguration t) {
		if (!m_templates.contains(t)) {
			m_templates.add(t);
			setChanged();
		}
	}

	public boolean contains(IEventConfiguration t) {
		return m_templates.contains(t);
	}

	public boolean replaceOriginalContentsFor(IEventConfiguration workingCopy) {
		EventConfiguration original = (EventConfiguration) workingCopy.getOriginal();
		if ((original != null) && original.replaceWithContentsFrom(workingCopy)) {
			original.save();
			setChanged();
			return true;
		}
		return false;
	}

	/*
	 * Since we always should notify observers after a change, use it to sort the templates first
	 * too.
	 *
	 * @see java.util.Observable#notifyObservers(java.lang.Object)
	 */
	@Override
	public void notifyObservers(Object arg) {
		Collections.sort(m_templates, COMPARATOR);
		super.notifyObservers(arg);
	}

	/**
	 * Check if the given name is sufficiently different from the names of all the templates in this
	 * repository to be considered unique.
	 *
	 * @param name
	 * @return true iff the given name is deemed unique
	 */
	public boolean isAllowedName(String name) {
		// FIXME: Go even further and ignore case and all whitespace differences?
		name = name.trim();
		for (IEventConfiguration rt : m_templates) {
			if (name.equals(rt.getName().trim())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Add the given template to this repository, after first ensuring that the name is unique, and
	 * then persisting the template. The template will only be added to the repository if it could
	 * be persisted.
	 *
	 * @param template
	 * @return true iff the given template was added to the repository (and thus was persisted)
	 */
	public boolean addAsUnique(IEventConfiguration template) {
		String newName = nextUniqueName(template.getName());
		template.setName(newName);
		if (template.save()) {
			add(template);
			return true;
		}
		return false;
	}

	/**
	 * Find a unique name for a template to add to this repository, using originalName as a initial
	 * name suggestion. If originalName is unique, it will be returned, possibly with some
	 * whitespace removed.
	 *
	 * @param originalName
	 * @return
	 */
	public String nextUniqueName(String originalName) {
		originalName = originalName.trim();

		// First, extract a base name and a count of the original name.
		String baseName = originalName;
		// Use count -1 to mean that no count should be appended, the baseName suffices.
		long proposedCount = -1;
		Matcher matcher = NAME_WITH_COUNT_PATTERN.matcher(originalName);
		if (matcher.matches()) {
			try {
				long count = Long.parseLong(matcher.group(2));
				// Valid match, use the shorter base and this count.
				baseName = matcher.group(1).trim();
				proposedCount = count;
			} catch (NumberFormatException e) {
				// Too large number. => Use the entire name as base.
				// (Yes, we could have used BigInteger, but which sane person would want such names?)
			}
		}

		// Second, find any existing templates matching the proposed baseName pattern,
		// with or without count, and make sure the proposed count is greater.
		int baseLen = baseName.length();
		for (IEventConfiguration template : m_templates) {
			String tempName = template.getName().trim();
			if (tempName.startsWith(baseName)) {
				if (tempName.equals(baseName) && (proposedCount < 1)) {
					proposedCount = 1;
				} else {
					// Note that this pattern must ignore leading whitespace.
					Matcher tempMatch = COUNT_SUFFIX_PATTERN.matcher(tempName.substring(baseLen));
					if (tempMatch.matches()) {
						try {
							long count = Long.parseLong(tempMatch.group(1));
							if (count < Long.MAX_VALUE) {
								// Valid match, use a count greater than this, unless the proposed was greater.
								proposedCount = Math.max(proposedCount, count + 1);
							}
						} catch (NumberFormatException e) {
							// Too large number, pretend we didn't see this template.
						}
					}
				}
			}
		}
		if (proposedCount == -1) {
			return baseName;
		} else {
			return baseName + " (" + proposedCount + ')'; //$NON-NLS-1$
		}
	}

	/**
	 * Set the prototype template used to create new recording templates. Template creation can be
	 * disabled by setting this to null (which also is the initial state of the repository).
	 *
	 * @param prototype
	 * @see #canCreateTemplates()
	 * @see #createTemplate()
	 */
	public void setPrototypeTemplate(IEventConfiguration prototype) {
		if (prototype != this.prototype) {
			this.prototype = prototype;
			setChanged();
		}
	}

	/**
	 * If this repository can create new recording templates. In other words, if it has a prototype
	 * template which it can clone to create new templates, since templates cannot be empty. Such a
	 * prototype template has to be explicitly provided to the repository by the
	 * {@link #setPrototypeTemplate(IEventConfiguration)} method.
	 *
	 * @return
	 * @see #createTemplate()
	 */
	public boolean canCreateTemplates() {
		return (prototype != null);
	}

	/**
	 * Creates a new recording template from the prototype template, if a prototype template has
	 * been set using {@link #setPrototypeTemplate(IEventConfiguration)}. Otherwise null is
	 * returned. Note that this template is not automatically added to the repository. It must be
	 * explicitly added using {@link #add(IEventConfiguration)} , possibly after being modified, to
	 * be maintained by the repository.
	 *
	 * @return a newly created recording template, or null
	 * @see #canCreateTemplates()
	 */
	public IEventConfiguration createTemplate() throws IOException {
		return (prototype != null) ? prototype.createCloneWithStorage(PrivateStorageDelegate.getDelegate()) : null;
	}

	public boolean isEmpty() {
		return m_templates.isEmpty();
	}

	public List<IEventConfiguration> getTemplates(SchemaVersion version) {
		if (version == null) {
			return Collections.unmodifiableList(m_templates);
		}
		return m_templates.stream().filter(rc -> rc.getVersion().equals(version)).collect(Collectors.toList());
	}
}
