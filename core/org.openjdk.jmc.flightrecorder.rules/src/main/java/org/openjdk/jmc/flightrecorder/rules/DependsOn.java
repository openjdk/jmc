package org.openjdk.jmc.flightrecorder.rules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DependsOn {

	public Class<? extends IRule2> value();

	public Severity severity() default Severity.OK;

}
