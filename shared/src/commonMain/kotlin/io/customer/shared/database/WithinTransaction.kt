package io.customer.shared.database

/**
 * Denotes that the annotated method should only be called inside database transaction.
 * This methods make it easier to set the expectations for caller to avoid any unwanted updates to
 * the database.
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
internal annotation class WithinTransaction
