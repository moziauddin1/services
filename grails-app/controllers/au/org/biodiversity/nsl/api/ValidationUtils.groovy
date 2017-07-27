package au.org.biodiversity.nsl.api

/**
 * User: pmcneil
 * Date: 20/04/17
 *
 */
trait ValidationUtils {

    static mustHave(Map things, Closure work) {
        things.each { k, v ->
            if (!v) {
                throw new IllegalArgumentException("$k must not be null")
            }
        }
        return work()
    }

    static mustHave(Map things) {
        things.each { k, v ->
            if (!v) {
                throw new IllegalArgumentException("$k must not be null")
            }
        }
    }
}