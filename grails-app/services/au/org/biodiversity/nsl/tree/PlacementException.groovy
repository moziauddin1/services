package au.org.biodiversity.nsl.tree

import org.springframework.http.HttpStatus

/**
 * User: pmcneil
 * Date: 24/05/17
 *
 */
class PlacementException extends Exception {

    HttpStatus status

    PlacementException(String message, HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR) {
        super(message)
        this.status = status
    }

}
