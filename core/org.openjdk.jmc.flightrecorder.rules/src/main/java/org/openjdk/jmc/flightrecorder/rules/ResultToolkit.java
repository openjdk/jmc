package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.util.TypedPreference;

public class ResultToolkit {

	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("(\\{.*?\\})"); //$NON-NLS-1$

	/**
	 * Populates the result message with result data.
	 * 
	 * @param result
	 *            the result to get the data from
	 * @param string
	 *            the message to populate
	 * @return a message populated with the formatted result values
	 */
	public static String populateMessage(IResult result, String string, boolean withHtml) {
		if (string == null) {
			return string;
		}
		String s = string;
		Matcher matcher = TEMPLATE_PATTERN.matcher(s);
		while (matcher.find()) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				String group = matcher.group(i);
				String subGroup = group.substring(1, group.length() - 1);
				TypedResult<?> typedResult = getResultById(result.getRule(), subGroup);
				if (typedResult != null) {
					if (typedResult instanceof TypedCollectionResult<?>) {
						if (withHtml) {
							StringBuilder collection = new StringBuilder();
							collection.append("<ul>"); //$NON-NLS-1$
							Collection<?> results = result.getResult((TypedCollectionResult<?>) typedResult);
							for (Object o : results) {
								collection.append("<li>"); //$NON-NLS-1$
								if (o instanceof IDisplayable) {
									collection.append(((IDisplayable) o).displayUsing(IDisplayable.AUTO));
								} else {
									collection.append(typedResult.format(o));
								}
								collection.append("</li>"); //$NON-NLS-1$
							}
							collection.append("</ul>"); //$NON-NLS-1$
							s = s.replace(group, collection.toString());
						} else {
							
						}
					} else {
						Object typedResultInstance = result.getResult(typedResult);
						if (typedResultInstance != null) {
							if (typedResultInstance instanceof IDisplayable) {
								s = s.replace(group, ((IDisplayable) typedResultInstance).displayUsing(IDisplayable.AUTO));
							} else {
								s = s.replace(group, typedResult.format(typedResultInstance));
							}
						}
					}
				} else {
					TypedPreference<?> typedPreference = getPreferenceById(result.getRule(), subGroup);
					if (typedPreference != null) {
						Object preference = result.getPreference(typedPreference);
						if (preference instanceof IDisplayable) {
							s = s.replace(group, ((IDisplayable) preference).displayUsing(IDisplayable.AUTO));
						} else {
							s = s.replace(group, preference.toString());
						}
					}
				}
			}
		}
		return s;
	}

	private static TypedResult<?> getResultById(IRule rule, String identifier) {
		Collection<TypedResult<?>> results = rule.getResults();
		if (results != null) {
			for (TypedResult<?> typedResult : results) {
				if (typedResult.getIdentifier().equals(identifier)) {
					return typedResult;
				}
			}
		}
		return null;
	}
	
	private static TypedPreference<?> getPreferenceById(IRule rule, String identifier) {
		Collection<TypedPreference<?>> preferences = rule.getConfigurationAttributes();
		if (preferences != null) {
			for (TypedPreference<?> typedPreference : preferences) {
				if (typedPreference.getIdentifier().equals(identifier)) {
					return typedPreference;
				}
			}
		}
		return null;
	}

}
