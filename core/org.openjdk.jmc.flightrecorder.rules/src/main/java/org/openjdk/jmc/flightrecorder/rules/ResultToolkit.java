package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;
import org.owasp.encoder.Encode;

public class ResultToolkit {

	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("(\\{.*?\\})"); //$NON-NLS-1$
	private static final FrameSeparator DEFAULT_SEPARATOR = new FrameSeparator(FrameCategorization.LINE, false);

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
									collection
											.append(Encode.forHtml(((IDisplayable) o).displayUsing(IDisplayable.AUTO)));
								} else {
									collection.append(Encode.forHtml(typedResult.format(o)));
								}
								collection.append("</li>"); //$NON-NLS-1$
							}
							collection.append("</ul>"); //$NON-NLS-1$
							s = s.replace(group, collection.toString());
						} else {
							Collection<?> results = result.getResult((TypedCollectionResult<?>) typedResult);
							String joined = StringToolkit.join(results, ","); //$NON-NLS-1$
							s = s.replace(group, encodeIfNeeded(joined, withHtml));
						}
					} else {
						Object typedResultInstance = result.getResult(typedResult);
						if (typedResultInstance != null) {
							if (typedResultInstance instanceof IDisplayable) {
								s = s.replace(group,
										encodeIfNeeded(
												((IDisplayable) typedResultInstance).displayUsing(IDisplayable.AUTO),
												withHtml));
							} else if (typedResultInstance instanceof IMCFrame) {
								s = s.replace(group, encodeIfNeeded(StacktraceFormatToolkit
										.formatFrame((IMCFrame) typedResultInstance, DEFAULT_SEPARATOR), withHtml));
							} else {
								s = s.replace(group, encodeIfNeeded(typedResult.format(typedResultInstance), withHtml));
							}
						}
					}
				} else {
					TypedPreference<?> typedPreference = getPreferenceById(result.getRule(), subGroup);
					if (typedPreference != null) {
						Object preference = result.getPreference(typedPreference);
						if (preference instanceof IDisplayable) {
							s = s.replace(group, encodeIfNeeded(
									((IDisplayable) preference).displayUsing(IDisplayable.AUTO), withHtml));
						} else {
							s = s.replace(group, encodeIfNeeded(preference.toString(), withHtml));
						}
					}
				}
			}
		}
		return s;
	}

	private static String encodeIfNeeded(String input, boolean shouldEncode) {
		if (shouldEncode) {
			return Encode.forHtml(input);
		}
		return input;
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
