package it.simonedegiacomi.goboxapi.myws.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This notation let you to implement a new query handler
 * and specify the query name in the same implementation
 *
 * Created by Degiacomi Simone on 07/02/16.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WSQuery {
    String name() default "";
}
