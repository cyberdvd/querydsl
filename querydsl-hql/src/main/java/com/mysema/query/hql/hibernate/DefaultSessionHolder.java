/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.hql.hibernate;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import com.mysema.commons.lang.Assert;

/**
 * DefaultSessionHolder is the default implementation of the SessionHolder interface
 * 
 * @author tiwe
 *
 */
public class DefaultSessionHolder implements SessionHolder{

    private final Session session;

    public DefaultSessionHolder(Session session){
        this.session = Assert.notNull(session,"session");
    }

    @Override
    public Query createQuery(String queryString) {
        return session.createQuery(queryString);
    }

    @Override
    public SQLQuery createSQLQuery(String queryString) {
        return session.createSQLQuery(queryString);
    }

}