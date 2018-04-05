package au.org.biodiversity.nsl

import org.joda.time.DateTime

import java.sql.Timestamp

/**
 * User: pmcneil
 * Date: 4/04/18
 *
 */
class ProfileValue {

    String value
    String createdAt
    String createdBy
    String updatedAt
    String updatedBy
    String errataReason
    String sourceLink
    ProfileValue previous

    ProfileValue(String value, String userName) {
        this.value = value
        createdAt = updatedAt = new Timestamp(System.currentTimeMillis())
        createdBy = updatedBy = userName
    }

    ProfileValue(String value, String userName, Map previousVal, String reason) {
        this.value = value
        DateTime now = new DateTime()
        String nowStr = now.toDateTimeISO()
        createdAt = updatedAt = nowStr
        createdBy = updatedBy = userName
        errataReason = reason
        if (previousVal) {
            previous = new ProfileValue(previousVal)
            createdBy = this.previous.createdBy
            createdAt = this.previous.createdAt
        }
    }

    ProfileValue(Map comment) {
        value = comment.value
        createdAt = comment.created_at
        createdBy = comment.created_by
        updatedAt = comment.updated_at
        updatedBy = comment.updated_by
        errataReason = comment.errata_reason
        sourceLink = comment.source_link
        if (comment.previous_comment) {
            previous = new ProfileValue(comment.previous_comment as Map)
        }
    }

    Map toMap() {
        [
                value           : value,
                created_at      : createdAt,
                created_by      : createdBy,
                updated_at      : updatedAt,
                updated_by      : updatedBy,
                errata_reason   : errataReason,
                source_link     : sourceLink,
                previous_comment: previous?.toMap()
        ]
    }

}
