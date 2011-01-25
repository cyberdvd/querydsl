/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.jpa;

import com.mysema.query.types.CollectionExpression;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Ops;
import com.mysema.query.types.expr.ComparableExpression;
import com.mysema.query.types.expr.NumberExpression;
import com.mysema.query.types.expr.ComparableOperation;
import com.mysema.query.types.expr.NumberOperation;
import com.mysema.query.types.expr.SimpleOperation;

/**
 * JPQLGrammar provides factory methods for JPQL specific operations
 * elements.
 *
 * @author tiwe
 * @version $Id$
 */
@SuppressWarnings("unchecked")
public final class JPQLGrammar {

    private JPQLGrammar(){}
    
    public static <D> Expression<D> all(CollectionExpression<?,D> col) {
        return SimpleOperation.create((Class)col.getParameter(0), Ops.QuantOps.ALL, (Expression<?>)col);
    }

    public static <D> Expression<D> any(CollectionExpression<?,D> col) {
        return SimpleOperation.create((Class)col.getParameter(0), Ops.QuantOps.ANY, (Expression<?>)col);
    }

    public static <A extends Comparable<? super A>> ComparableExpression<A> avg(CollectionExpression<?,A> col) {
        return ComparableOperation.create((Class)col.getParameter(0), Ops.QuantOps.AVG_IN_COL, (Expression<?>)col);
    }

    public static <A extends Comparable<? super A>> ComparableExpression<A> max(CollectionExpression<?,A> left) {
        return ComparableOperation.create((Class)left.getParameter(0), Ops.QuantOps.MAX_IN_COL, (Expression<?>)left);
    }

    public static <A extends Comparable<? super A>> ComparableExpression<A> min(CollectionExpression<?,A> left) {
        return ComparableOperation.create((Class)left.getParameter(0), Ops.QuantOps.MIN_IN_COL, (Expression<?>)left);
    }

    public static <D> Expression<D> some(CollectionExpression<?,D> col) {
        return any(col);
    }

    /**
     * SUM returns Long when applied to state-fields of integral types (other
     * than BigInteger); Double when applied to state-fields of floating point
     * types; BigInteger when applied to state-fields of type BigInteger; and
     * BigDecimal when applied to state-fields of type BigDecimal.
     */
    public static <D extends Number & Comparable<? super D>> NumberExpression<?> sum(Expression<D> left) {
        Class<?> type = left.getType();
        if (type.equals(Byte.class) || type.equals(Integer.class) || type.equals(Short.class)){
            type = Long.class;
        }else if (type.equals(Float.class)){
            type = Double.class;
        }
        return NumberOperation.create((Class<D>) type, Ops.AggOps.SUM_AGG, left);
    }

    public static <D extends Number & Comparable<? super D>> NumberExpression<Long> sumAsLong(Expression<D> left) {
        return sum(left).longValue();
    }

    public static <D extends Number & Comparable<? super D>> NumberExpression<Double> sumAsDouble(Expression<D> left) {
        return sum(left).doubleValue();
    }

}