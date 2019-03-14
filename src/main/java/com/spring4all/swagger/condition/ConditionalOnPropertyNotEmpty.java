package com.spring4all.swagger.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * Created by zhangleimin on 2019/3/13.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnPropertyNotEmptyConditional.class)
public @interface ConditionalOnPropertyNotEmpty {

    String name() default "";
}
