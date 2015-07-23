package au.org.biodiversity.nsl;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * User: pmcneil
 * Date: 18/05/15
 */
public class ExtendedPostgreSQLDialect extends org.hibernate.dialect.PostgreSQL82Dialect {
    public ExtendedPostgreSQLDialect() {
        super();
        registerFunction("regex", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "(?1 ~ ?2)"));
        registerFunction("iregex", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "(?1 ~* ?2)"));
        registerFunction("not_regex", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "(?1 !~ ?2)"));
        registerFunction("not_iregex", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "(?1 !~* ?2)"));
    }
}
