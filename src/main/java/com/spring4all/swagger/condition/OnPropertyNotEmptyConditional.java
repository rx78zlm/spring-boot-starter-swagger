package com.spring4all.swagger.condition;

import com.google.common.base.Strings;
import com.spring4all.swagger.utils.StringUtil;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Created by zhangleimin on 2019/3/13.
 */
public class OnPropertyNotEmptyConditional extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Object propertiesName = metadata.getAnnotationAttributes(ConditionalOnPropertyNotEmpty.class.getName()).get("name");
        if (propertiesName != null) {
            String value = context.getEnvironment().getProperty(StringUtil.underscoreName(propertiesName.toString()));
            if (!Strings.isNullOrEmpty(value)) {
                return new ConditionOutcome(true, "get properties");
            }
            value = context.getEnvironment().getProperty(StringUtil.camelCaseName(propertiesName.toString()));
            if (!Strings.isNullOrEmpty(value)) {
                return new ConditionOutcome(true, "get properties");
            }
        }
        return new ConditionOutcome(false, "none get properties");
    }
}
