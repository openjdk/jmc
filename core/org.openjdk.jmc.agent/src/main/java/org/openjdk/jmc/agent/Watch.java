package org.openjdk.jmc.agent;

import org.openjdk.jmc.agent.util.expression.ExpressionResolver;
import org.openjdk.jmc.agent.util.expression.IllegalSyntaxException;
import org.openjdk.jmc.agent.util.expression.ReferenceChain;
import org.openjdk.jmc.agent.util.TypeUtils;

public class Watch implements IAttribute {
    
    private final String name;
    private final String expression;
    private final String fieldName;
    private final String description;
    private final String contentType;
    private final String relationKey;
    private final String converterClassName;
    
    public Watch(String name, String expression, String description, String contentType, String relationKey, String converterClassName) {
        this.name = name;
        this.expression = expression;
        this.description = description;
        this.contentType = contentType;
        this.relationKey = relationKey;
        this.converterClassName = converterClassName;
        this.fieldName = "field" + TypeUtils.deriveIdentifierPart(name);
        
        // TODO: validate expression
    }
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getFieldName() {
        return this.fieldName;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public String getRelationKey() {
        return this.relationKey;
    }

    @Override
    public String getConverterClassName() {
        return this.converterClassName;
    }
    
    public ReferenceChain resolveReferenceChain(Class<?> callerClass, boolean fromStaticContext) throws IllegalSyntaxException {
        return new ExpressionResolver(callerClass, expression, fromStaticContext).solve();
    }
}
