package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.util.StringToolkit;

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
	public static String populateMessage(IResult result, String string) {
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
						Collection<?> results = result.getResult((TypedCollectionResult<?>) typedResult);
						String joined = StringToolkit.join(results, ","); //$NON-NLS-1$
						s = s.replace(group, joined);
					} else {
						Object typedResultInstance = result.getResult(typedResult);
						if (typedResultInstance != null) {
							if (typedResultInstance instanceof IDisplayable) {
								s = s.replace(group, ((IDisplayable) typedResultInstance).displayUsing(IDisplayable.AUTO));
							} else {
								s = s.replace(group, typedResultInstance.toString());
							}
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

}
