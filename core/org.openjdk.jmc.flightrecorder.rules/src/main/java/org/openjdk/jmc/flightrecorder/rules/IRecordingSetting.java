package org.openjdk.jmc.flightrecorder.rules;

import java.io.Serializable;

public interface IRecordingSetting extends Serializable {

	String getSettingFor();

	String getSettingName();

	String getSettingValue();

}
