package it.simonedegiacomi.goboxapi.myws.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to specify the name of the
 * event that the handler you are implementing need
 * to respond.
 *
 * Created by Degiacomi Simone onEvent 07/02/16.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WSEvent {
    String name() default "";
}
