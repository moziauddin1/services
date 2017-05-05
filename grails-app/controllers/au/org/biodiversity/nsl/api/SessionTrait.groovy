package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.tree.DomainUtils
import org.hibernate.SessionFactory

/**
 * User: pmcneil
 * Date: 5/05/17
 *
 */
trait SessionTrait {

    SessionFactory sessionFactory_nsl

    Object clearAndFlush(Closure work) {
        assert sessionFactory_nsl
        if (sessionFactory_nsl.getCurrentSession().isDirty()) {
            throw new IllegalStateException("Changes to the classification trees may only be done via BasicOperationsService")
        }
        sessionFactory_nsl.getCurrentSession().clear()
        // I don't use a try/catch because if an exception is thrown then meh
        Object ret = work()
        sessionFactory_nsl.getCurrentSession().flush()
        sessionFactory_nsl.getCurrentSession().clear()
        return DomainUtils.refetchObject(ret)
    }

}