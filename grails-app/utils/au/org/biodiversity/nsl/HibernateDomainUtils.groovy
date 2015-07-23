package au.org.biodiversity.nsl

import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy

/**
 * User: pmcneil
 * Date: 31/03/15
 *
 */
class HibernateDomainUtils {

    static <T> T initializeAndUnproxy(T entity) {
        if (entity == null) {
            throw new NullPointerException("Entity passed for initialization is null");
        }

        Hibernate.initialize(entity);
        if (entity instanceof HibernateProxy) {
            entity = (T) ((HibernateProxy) entity).getHibernateLazyInitializer()
                                                  .getImplementation();
        }
        return entity;
    }


}
