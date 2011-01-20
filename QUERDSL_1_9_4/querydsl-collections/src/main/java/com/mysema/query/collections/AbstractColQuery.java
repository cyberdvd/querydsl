/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.query.JoinType;
import com.mysema.query.QueryException;
import com.mysema.query.QueryMetadata;
import com.mysema.query.SearchResults;
import com.mysema.query.support.ProjectableQuery;
import com.mysema.query.support.QueryMixin;
import com.mysema.query.types.Expr;
import com.mysema.query.types.Ops;
import com.mysema.query.types.Path;
import com.mysema.query.types.expr.EArrayConstructor;
import com.mysema.query.types.expr.OSimple;
import com.mysema.query.types.path.PMap;

/**
 * AbstractColQuery provides a base class for Collection query implementations.
 * Extend it like this
 *
 * <pre>
 * public class MyType extends AbstractColQuery&lt;MyType&gt;{
 *   ...
 * }
 * </pre>
 *
 * @see ColQuery
 *
 * @author tiwe
 * @version $Id$
 */
public abstract class AbstractColQuery<Q extends AbstractColQuery<Q>>  extends ProjectableQuery<Q> {

    private final Map<Expr<?>, Iterable<?>> iterables = new HashMap<Expr<?>, Iterable<?>>();

    private final QueryEngine queryEngine;

    @SuppressWarnings("unchecked")
    public AbstractColQuery(QueryMetadata metadata, QueryEngine queryEngine) {
        super(new QueryMixin<Q>(metadata));
        this.queryMixin.setSelf((Q) this);
        this.queryEngine = queryEngine;
    }

    @Override
    public long count() {
        try {
            return queryEngine.count(getMetadata(), iterables);
        } catch (Exception e) {
            throw new QueryException(e.getMessage(), e);
        }finally{
            reset();
        }
    }

    @SuppressWarnings("unchecked")
    private <D> Expr<D> createAlias(Path<? extends Collection<D>> target, Path<D> alias){
        return OSimple.create((Class<D>)alias.getType(), Ops.ALIAS, target.asExpr(), alias.asExpr());
    }

    @SuppressWarnings("unchecked")
    private <D> Expr<D> createAlias(PMap<?,D,?> target, Path<D> alias){
        return OSimple.create((Class<D>)alias.getType(), Ops.ALIAS, target.asExpr(), alias.asExpr());
    }

    @SuppressWarnings("unchecked")
    public <A> Q from(Path<A> entity, Iterable<? extends A> col) {
        iterables.put(entity.asExpr(), col);
        getMetadata().addJoin(JoinType.DEFAULT, entity.asExpr());
        return (Q)this;
    }

    public abstract QueryMetadata getMetadata();

    protected QueryEngine getQueryEngine(){
        return queryEngine;
    }

    @SuppressWarnings("unchecked")
    public <P> Q innerJoin(Path<? extends Collection<P>> target, Path<P> alias) {
        getMetadata().addJoin(JoinType.INNERJOIN, createAlias(target, alias));
        return (Q)this;
    }

    @SuppressWarnings("unchecked")
    public <P> Q innerJoin(PMap<?,P,?> target, Path<P> alias) {
        getMetadata().addJoin(JoinType.INNERJOIN, createAlias(target, alias));
        return (Q)this;
    }

    @Override
    public CloseableIterator<Object[]> iterate(Expr<?>[] args) {
        return iterate(new EArrayConstructor<Object>(args));
    }

    @Override
    public <RT> CloseableIterator<RT> iterate(Expr<RT> projection) {
        try {
            queryMixin.addToProjection(projection);
            return new IteratorAdapter<RT>(queryEngine.list(getMetadata(), iterables, projection).iterator());
        }finally{
            reset();
        }
    }

    @Override
    public List<Object[]> list(Expr<?>[] args) {
        return list(new EArrayConstructor<Object>(args));
    }

    @Override
    public <RT> List<RT> list(Expr<RT> projection) {
        try {
            queryMixin.addToProjection(projection);
            return queryEngine.list(getMetadata(), iterables, projection);
        }finally{
            reset();
        }
    }

    @Override
    public <RT> SearchResults<RT> listResults(Expr<RT> projection) {
        queryMixin.addToProjection(projection);
        long count = queryEngine.count(getMetadata(), iterables);
        if (count > 0l){
            List<RT> list = queryEngine.list(getMetadata(), iterables, projection);
            reset();
            return new SearchResults<RT>(list, getMetadata().getModifiers(), count);
        }else{
            reset();
            return SearchResults.<RT>emptyResults();
        }

    }

    private void reset(){
        getMetadata().reset();
    }

}