package org.apache.skywalking.apm.agent.core.plugin.bytebuddy;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * @author yunhai.hu
 * at 2019/3/8
 */
public class ReturnTypeNameMatch implements ElementMatcher<MethodDescription> {

    private String typeName;

    private ReturnTypeNameMatch(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public boolean matches(MethodDescription target) {
        return target.getReturnType().getTypeName().equals(typeName);
    }

    public static ElementMatcher<MethodDescription> takesReturnTypeNameMatch(String typeName) {
        return new ReturnTypeNameMatch(typeName);
    }
}
