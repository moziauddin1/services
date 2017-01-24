package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.tree.Message
import au.org.biodiversity.nsl.tree.ServiceException
import grails.converters.JSON
import org.hibernate.JDBCException

import java.sql.SQLException

/**
 * This utility class recurses through a top-level tree services message object
 * and creates the 'displayMessage' array. This makes everything simple.
 *
 * Created by ibis on 3/1/17.
 */
class TreeServiceMessageUtil {
    final LinkService linkService;

    TreeServiceMessageUtil(LinkService linkService) {
        this.linkService = linkService;
    }

    List unpackMessage(Message m, status = "danger") {
        List msg = [];
        if (m != null) {
            msg << [msg: m.getHumanReadableMessage(), status: status, args: unrollArgs(m)]
            unrollAllNested(msg, m)
        }
        return msg
    }

    List unpackThrowable(Throwable t, String status = 'danger') {
        if (t == null) {
            return []
        } else if (t instanceof ServiceException) {
            return unpackServiceException((ServiceException) t, status);
        } else if (t instanceof JDBCException) {
            return unpackJDBCException((JDBCException) t, status);
        } else {
            return unpackOtherException(t, status);
        }

    }

    private List unpackJDBCException(JDBCException ex, String status) {
        List msg = []

        if (ex != null) {
            msg << [msg: ex.getClass().getSimpleName(), body: ex.getLocalizedMessage(), status: status]

            for (SQLException sex = ex.getSQLException(); sex != null; sex = sex.getNextException()) {
                msg += unpackThrowable(sex, 'info')
            }
        }
        return msg
    }

    private List unpackServiceException(ServiceException ex, String status) {
        List msg = []
        if (ex != null) {
            msg += unpackMessage(ex.msg, status)
            if (ex.getCause() && ex.getCause() != ex) {
                msg += unpackThrowable(ex.getCause(), status);
            }
        }
        return msg
    }

    private List unpackOtherException(Throwable t, String status) {
        if (t == null) return [];

        List msg = [
                [msg: t.getClass().getSimpleName(), body: t.getLocalizedMessage(), status: status]
        ]
        if (t.getCause() && t.getCause() != t) {
            msg += unpackThrowable(t.getCause(), status);
        }
        return msg;
    }

    List unpackStacktrace(Throwable t) {
        if (t == null) return [];
        return t.getStackTrace().findAll {
            StackTraceElement it -> it.fileName && it.lineNumber != -1 && it.className.startsWith('au.org.biodiversity.nsl.')
        }.collect {
            StackTraceElement it -> [file: it.fileName, line: it.lineNumber, method: it.methodName, clazz: it.className]
        }
    }

    private void unrollAllNested(List msg, Message m) {
        if (m == null) return;

        for (Message mm : m.nested) {
            msg << [msg: mm.getHumanReadableMessage(), status: 'info', args: unrollArgs(m)]
            unrollAllNested(msg, mm)
        }
    }

    List unrollArgs(Message msg) {
        List l = [];

        if (msg) {
            msg.args.each {
                if (it instanceof Message) {
                    l << unrollArgs((Message) it)
                } else if (it instanceof Name
                        || it instanceof Instance
                        || it instanceof Reference
                        || it instanceof Author
                        || it instanceof InstanceNote
                        || it instanceof Node
                        || it instanceof Arrangement
                        || it instanceof Event
                ) {
                    l << [class: it.class, uri: linkService.getPreferredLinkForObject(it)]
                } else {
                    l << it;
                }
            }

        }

        return l;
    }

}
