package io.customer.shared.database

/**
 * Denotes that the annotated method should only be called on a background dispatcher.
 * If the annotated element is a class, then all methods in the class should be called
 * on background dispatcher.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.SOURCE)
annotation class WithinTransaction
