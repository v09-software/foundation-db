/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.server.types3.common.types;

import com.akiban.server.types3.Attribute;
import com.akiban.server.types3.TBundleID;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TFactory;
import com.akiban.server.types3.TInstance;
import com.akiban.server.types3.TParser;
import com.akiban.server.types3.*;
import com.akiban.server.types3.pvalue.PUnderlying;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.sql.types.TypeId;

public class NoAttrTClass extends SimpleDtdTClass {

    @Override
    public TFactory factory() {
        return new NoAttrFactory(instance());
    }

    @Override
    protected TInstance doPickInstance(TInstance instance0, TInstance instance1) {
        return instance0; // doesn't matter which it is
    }

    @Override
    public TInstance instance() {
        return createInstanceNoArgs();
    }

    @Override
    protected void validate(TInstance instance) {
    }

    public NoAttrTClass(TBundleID bundle, String name, TClassFormatter formatter, int internalRepVersion,
                           int serializationVersion, int serializationSize, PUnderlying pUnderlying, TParser parser, TypeId typeId) {
        super(bundle, name, formatter, Attribute.NONE.class, internalRepVersion, serializationVersion, serializationSize,
                pUnderlying, parser, typeId);
    }

    @Override
    public void putSafety(TExecutionContext context, 
                          TInstance sourceInstance,
                          PValueSource sourceValue,
                          TInstance targetInstance,
                          PValueTarget targetValue)
    {
        // nothing to check ?
        targetValue.putValueSource(sourceValue);
    }
}
