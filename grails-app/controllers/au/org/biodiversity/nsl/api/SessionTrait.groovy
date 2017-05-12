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

    void cleanSession() {
        sessionFactory_nsl.getCurrentSession().flush()
    }

    /**
     * This checks for domain objects and if they've become detatched from the session it re-attaches them.
     * This can happen for a couple of reasons, but here it is most likely because of the cleanAndFlush ing that goes on
     * all over the place. We'll slowly remove these things and then we may not need this.
     *
     * This simply looks at the collection of values, it doesn't recurse at all.
     *
     * @param things - A map of things.
     */
    private mapAttach(Map things) {
        for (thing in things.values()) {
            if (grailsApplication.isDomainClass(thing.getClass()) && !thing.isAttached()) {
                thing.attach()
            }
        }
    }
}