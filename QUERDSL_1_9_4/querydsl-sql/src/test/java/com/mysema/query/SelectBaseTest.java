/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query;

import static com.mysema.query.Constants.date;
import static com.mysema.query.Constants.employee;
import static com.mysema.query.Constants.employee2;
import static com.mysema.query.Constants.survey;
import static com.mysema.query.Constants.survey2;
import static com.mysema.query.Constants.time;
import static com.mysema.query.Target.DERBY;
import static com.mysema.query.Target.H2;
import static com.mysema.query.Target.HSQLDB;
import static com.mysema.query.Target.MYSQL;
import static com.mysema.query.Target.ORACLE;
import static com.mysema.query.Target.SQLSERVER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.commons.lang.Pair;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSerializer;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.domain.IdName;
import com.mysema.query.sql.domain.QEmployee;
import com.mysema.query.sql.domain.QIdName;
import com.mysema.query.types.EConstructor;
import com.mysema.query.types.Expr;
import com.mysema.query.types.Param;
import com.mysema.query.types.ParamNotSetException;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.expr.Coalesce;
import com.mysema.query.types.expr.EArrayConstructor;
import com.mysema.query.types.expr.EBoolean;
import com.mysema.query.types.expr.ENumber;
import com.mysema.query.types.expr.QTuple;
import com.mysema.query.types.path.PNumber;
import com.mysema.query.types.path.PathBuilder;
import com.mysema.query.types.query.ListSubQuery;
import com.mysema.query.types.query.ObjectSubQuery;
import com.mysema.testutil.ExcludeIn;
import com.mysema.testutil.Label;

public abstract class SelectBaseTest extends AbstractBaseTest{

    public static class SimpleProjection {

    public SimpleProjection(String str, String str2) {
        }

    }

    private QueryExecution standardTest = new QueryExecution(Module.SQL, getClass().getAnnotation(Label.class).value()){
        @Override
        protected Pair<Projectable, List<Expr<?>>> createQuery() {
            return Pair.of(
                (Projectable)query().from(employee, employee2),
                Collections.<Expr<?>>emptyList());
        }
        @Override
        protected Pair<Projectable, List<Expr<?>>> createQuery(EBoolean filter) {
            return Pair.of(
                (Projectable)query().from(employee, employee2).where(filter),
                Collections.<Expr<?>>singletonList(employee.firstname));
        }
    };

    @Test
    public void aggregate(){
        int min = 30000, avg = 65000, max = 160000;

        // uniqueResult
        assertEquals(min, query().from(employee).uniqueResult(employee.salary.min()).intValue());

        assertEquals(avg, query().from(employee).uniqueResult(employee.salary.avg()).intValue());

        assertEquals(max, query().from(employee).uniqueResult(employee.salary.max()).intValue());

        // list
        assertEquals(min, query().from(employee).list(employee.salary.min()).get(0).intValue());

        assertEquals(avg, query().from(employee).list(employee.salary.avg()).get(0).intValue());

        assertEquals(max, query().from(employee).list(employee.salary.max()).get(0).intValue());
    }

    @Test
    public void coalesce(){
        Coalesce<String> c = new Coalesce<String>(employee.firstname, employee.lastname).add("xxx");
        query().from(employee).where(c.eq("xxx")).list(employee.id);
    }

    @Test
    @ExcludeIn({DERBY,MYSQL})
    public void casts() throws SQLException {
        ENumber<?> num = employee.id;
        Expr<?>[] expr = new Expr[] {
                num.byteValue(),
                num.doubleValue(),
                num.floatValue(),
                num.intValue(),
                num.longValue(),
                num.shortValue(),
                num.stringValue() };

        for (Expr<?> e : expr) {
            for (Object o : query().from(employee).list(e)){
                assertEquals(e.getType(), o.getClass());
            }
        }

    }

    @Test
    public void constructor() throws Exception {
        for (IdName idName : query().from(survey).list(new QIdName(survey.id, survey.name))) {
            System.out.println("id and name : " + idName.getId() + ","+ idName.getName());
        }
    }

    @Test
    public void getResultSet() throws IOException, SQLException{
        ResultSet results = query().from(survey).getResults(survey.id, survey.name);
        while(results.next()){
            System.out.println(results.getInt(1) +","+results.getString(2));
        }
        results.close();
    }

    @SuppressWarnings("unchecked")
    @Test(expected=IllegalArgumentException.class)
    public void illegalUnion() throws SQLException {
        SubQueryExpression<Integer> sq1 = sq().from(employee).unique(employee.id.max());
        SubQueryExpression<Integer> sq2 = sq().from(employee).unique(employee.id.max());
        query().from(employee).union(sq1, sq2).list();
    }

    @Test
    public void join() throws Exception {
        for (String name : query().from(survey, survey2)
            .where(survey.id.eq(survey2.id)).list(survey.name)) {
            System.out.println(name);
        }
    }

    @Test
    public void joins() throws SQLException {
        for (Object[] row : query().from(employee).innerJoin(employee2)
                .on(employee.superiorId.eq(employee2.superiorId))
                .where(employee2.id.eq(10))
                .list(employee.id, employee2.id)) {
            System.out.println(row[0] + ", " + row[1]);
        }
    }

    @Test
    public void compactJoin(){
        // verbose
        query().from(employee).innerJoin(employee2)
            .on(employee.superiorId.eq(employee2.id))
            .list(employee.id, employee2.id);

        // compact
        query().from(employee)
            .innerJoin(employee.superiorIdKey, employee2)
            .list(employee.id, employee2.id);

    }

    @Test
    public void limitAndOffset() throws SQLException {
        // limit
        query().from(employee)
            .orderBy(employee.firstname.asc())
            .limit(4).list(employee.id);

        // limit and offset
        query().from(employee)
            .orderBy(employee.firstname.asc())
            .limit(4).offset(3).list(employee.id);
    }

    @Test
    @ExcludeIn({ORACLE,DERBY,SQLSERVER})
    public void limitAndOffset2() throws SQLException {
        // limit
        expectedQuery = "select e.ID from EMPLOYEE2 e limit ?";
        query().from(employee).limit(4).list(employee.id);

        // limit offset
        expectedQuery = "select e.ID from EMPLOYEE2 e limit ? offset ?";
        query().from(employee).limit(4).offset(3).list(employee.id);

    }

    @Test
    public void limitAndOffsetAndOrder(){
        // limit
        List<String> names1 = Arrays.asList("Barbara","Daisy","Helen","Jennifer");
        assertEquals(names1, query().from(employee)
                .orderBy(employee.firstname.asc())
                .limit(4)
                .list(employee.firstname));

        // limit + offset
        List<String> names2 = Arrays.asList("Helen","Jennifer","Jim","Joe");
        assertEquals(names2, query().from(employee)
                .orderBy(employee.firstname.asc())
                .limit(4).offset(2)
                .list(employee.firstname));
    }

//    @SuppressWarnings("unchecked")
//    @Test
//    @ExcludeIn({DERBY})
//    public void mathFunctions() throws SQLException {
//        Expr<Double> d = ENumberConst.create(1.0);
//        for (Expr<?> e : Arrays.<Expr<?>> asList(
//                MathFunctions.acos(d),
//                MathFunctions.asin(d),
//                MathFunctions.atan(d),
//                MathFunctions.cos(d),
//                MathFunctions.tan(d),
//                MathFunctions.sin(d),
//                ENumber.random(),
//                MathFunctions.pow(d, d),
//                MathFunctions.log10(d),
//                MathFunctions.log(d),
//                MathFunctions.exp(d))) {
//            query().from(employee).list((Expr<? extends Comparable>) e);
//        }
//    }

    @Test
    @ExcludeIn({HSQLDB, H2, MYSQL})
    public void offsetOnly(){
        // offset
        query().from(employee)
            .orderBy(employee.firstname.asc())
            .offset(3).list(employee.id);
    }

    @Test
    public void projection() throws IOException{
        // TODO : add assertions
        CloseableIterator<Object[]> results = query().from(survey).iterate(survey.all());
        assertTrue(results.hasNext());
        while (results.hasNext()){
            assertEquals(2, results.next().length);
        }
        results.close();
    }

    @Test
    public void projection2() throws IOException{
        // TODO : add assertions
        CloseableIterator<Object[]> results = query().from(survey).iterate(survey.id, survey.name);
        assertTrue(results.hasNext());
        while (results.hasNext()){
            assertEquals(2, results.next().length);
        }
        results.close();
    }

    @Test
    public void projection3() throws IOException{
        CloseableIterator<String> names = query().from(survey).iterate(survey.name);
        assertTrue(names.hasNext());
        while (names.hasNext()){
            System.out.println(names.next());
        }
        names.close();
    }

    @Test
    public void query1() throws Exception {
        for (String s : query().from(survey).list(survey.name)) {
            System.out.println(s);
        }
    }

    @Test
    public void query2() throws Exception {
        for (Object[] row : query().from(survey).list(survey.id, survey.name)) {
            System.out.println(row[0] + ", " + row[1]);
        }
    }

    @Test
    public void queryWithConstant() throws Exception {
        for (Object[] row : query().from(survey)
                .where(survey.id.eq(1))
                .list(survey.id, survey.name)) {
            System.out.println(row[0] + ", " + row[1]);
        }
    }

    @Test
    @Ignore
    @ExcludeIn({ORACLE, DERBY, SQLSERVER})
    public void selectBooleanExpr() throws SQLException {
        // TODO : FIXME
        System.out.println(query().from(survey).list(survey.id.eq(0)));
    }

    @Test
    @Ignore
    @ExcludeIn({ORACLE, DERBY, SQLSERVER})
    public void selectBooleanExpr2() throws SQLException {
        // TODO : FIXME
        System.out.println(query().from(survey).list(survey.id.gt(0)));
    }

    @Test
    public void selectConcat() throws SQLException {
        System.out.println(query().from(survey).list(survey.name.append("Hello World")));
    }

    @Test
    public void serialization(){
        SQLQuery query = query();

        query.from(survey);
        assertEquals("from SURVEY s", query.toString());

        query.from(survey2);
        assertEquals("from SURVEY s, SURVEY s2", query.toString());
    }

    @Test
    public void standardTest(){
        standardTest.runBooleanTests(employee.firstname.isNull(), employee2.lastname.isNotNull());
        standardTest.runDateTests(employee.datefield, employee2.datefield, date);

        // int
        standardTest.runNumericCasts(employee.id, employee2.id, 1);
        standardTest.runNumericTests(employee.id, employee2.id, 1);

        // BigDecimal
        standardTest.runNumericTests(employee.salary, employee2.salary, new BigDecimal("30000.00"));

        standardTest.runStringTests(employee.firstname, employee2.firstname, "Jennifer");
        standardTest.runTimeTests(employee.timefield, employee2.timefield, time);

        standardTest.report();
    }

    @Test
    public void stringFunctions2() throws SQLException {
        for (EBoolean where : Arrays.<EBoolean> asList(
                employee.firstname.startsWith("a"),
                employee.firstname.startsWithIgnoreCase("a"),
                employee.firstname.endsWith("a"),
                employee.firstname.endsWithIgnoreCase("a"))) {
            query().from(employee).where(where).list(employee.firstname);
        }
    }
    @Test
    public void subQueries() throws SQLException {
        // subquery in where block
        expectedQuery = "select e.ID from EMPLOYEE2 e "
                + "where e.ID = (select max(e.ID) "
                + "from EMPLOYEE2 e)";
        List<Integer> list = query().from(employee)
            .where(employee.id.eq(sq().from(employee).unique(employee.id.max())))
            .list(employee.id);
        assertFalse(list.isEmpty());
    }

    @Test
    public void subQuerySerialization(){
        SQLSubQuery query = sq();

        query.from(survey);
        assertEquals("from SURVEY s", query.toString());

        query.from(survey2);
        assertEquals("from SURVEY s, SURVEY s2", query.toString());

        PNumber<BigDecimal> sal = new PNumber<BigDecimal>(BigDecimal.class, "sal");
        PathBuilder<Object[]> sq = new PathBuilder<Object[]>(Object[].class, "sq");
        SQLSerializer serializer = new SQLSerializer(SQLTemplates.DEFAULT);
        serializer.handle(sq().from(employee)
            .list(employee.salary.add(employee.salary).add(employee.salary).as(sal)).as(sq));
        assertEquals("(select (e.SALARY + e.SALARY + e.SALARY) as sal\nfrom EMPLOYEE2 e) as sq", serializer.toString());
    }

    @Test
    public void complexSubQuery(){
//        query.from(
//            subQuery.from(Table.table)
//            .list(Table.table.field1.add(Table.table.field2).add(Table.table.field3).as("myCalculation"))).
//            list(myCalculation.avg(), myCalculation.min(). myCalculation.max());

        // alias for the salary
        PNumber<BigDecimal> sal = new PNumber<BigDecimal>(BigDecimal.class, "sal");
        // alias for the subquery
        PathBuilder<Object[]> sq = new PathBuilder<Object[]>(Object[].class, "sq");
        // query execution
        query().from(
                sq().from(employee)
                .list(employee.salary.add(employee.salary).add(employee.salary).as(sal)).as(sq)
              ).list(sq.get(sal).avg(), sq.get(sal).min(), sq.get(sal).max());

//        select avg(sq.sal), min(sq.sal), max(sq.sal) from (select (e.SALARY + e.SALARY + e.SALARY) as sal from EMPLOYEE2 e) as sq
    }

    @Test
    public void subQueryJoin(){
        ListSubQuery<Integer> sq = sq().from(employee2).list(employee2.id);
        QEmployee sqEmp = new QEmployee("sq");

        // inner join
        query().from(employee).innerJoin(sq, sqEmp).on(sqEmp.id.eq(employee.id)).list(employee.id);

        // left join
        query().from(employee).leftJoin(sq, sqEmp).on(sqEmp.id.eq(employee.id)).list(employee.id);

        // right join
        // FIXME
//        query().from(employee).rightJoin(sq, sqEmp).on(sqEmp.id.eq(employee.id)).list(employee.id);

        // FIXME
        // full join
//        query().from(employee).fullJoin(sq, sqEmp).on(sqEmp.id.eq(employee.id)).list(employee.id);

    }

    @Test
    public void syntaxForEmployee() throws SQLException {
        query().from(employee).groupBy(employee.superiorId)
            .orderBy(employee.superiorId.asc())
            .list(employee.salary.avg(),employee.id.max());

        query().from(employee).groupBy(employee.superiorId)
            .having(employee.id.max().gt(5))
            .orderBy(employee.superiorId.asc())
            .list(employee.salary.avg(), employee.id.max());

        query().from(employee).groupBy(employee.superiorId)
            .having(employee.superiorId.isNotNull())
            .orderBy(employee.superiorId.asc())
            .list(employee.salary.avg(),employee.id.max());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void union() throws SQLException {
        // union
        SubQueryExpression<Integer> sq1 = sq().from(employee).unique(employee.id.max());
        SubQueryExpression<Integer> sq2 = sq().from(employee).unique(employee.id.min());
        List<Integer> list = query().union(sq1, sq2).list();
        assertFalse(list.isEmpty());

        // variation 1
        list = query().union(
                sq().from(employee).unique(employee.id.max()),
                sq().from(employee).unique(employee.id.min())).list();
        assertFalse(list.isEmpty());

        // union #2
        ObjectSubQuery<Object[]> sq3 = sq().from(employee).unique(new Expr[]{employee.id.max()});
        ObjectSubQuery<Object[]> sq4 = sq().from(employee).unique(new Expr[]{employee.id.min()});
        List<Object[]> list2 = query().union(sq3, sq4).list();
        assertFalse(list2.isEmpty());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void union_single_column_projections() throws IOException{
        SubQueryExpression<Integer> sq1 = sq().from(employee).unique(employee.id.max());
        SubQueryExpression<Integer> sq2 = sq().from(employee).unique(employee.id.min());
        
        // list
        List<Integer> list = query().union(sq1, sq2).list();
        assertEquals(2, list.size());
        assertTrue(list.get(0) != null);
        assertTrue(list.get(1) != null);
        
        // iterator
        CloseableIterator<Integer> iterator = query().union(sq1,sq2).iterate();
        try{
            assertTrue(iterator.hasNext());   
            assertTrue(iterator.next() != null);
            assertTrue(iterator.next() != null);
            assertFalse(iterator.hasNext());
        }finally{
            iterator.close();
        }        
    }
    
    @Test
    public void union_multi_column_projection() throws IOException{
        SubQueryExpression<Object[]> sq1 = sq().from(employee).unique(employee.id.max(), employee.id.max().subtract(1));
        SubQueryExpression<Object[]> sq2 = sq().from(employee).unique(employee.id.min(), employee.id.min().subtract(1));
        
        // list
        List<Object[]> list = query().union(sq1, sq2).list();
        assertEquals(2, list.size());
        assertTrue(list.get(0) != null);
        assertTrue(list.get(1) != null);
        
        // iterator
        CloseableIterator<Object[]> iterator = query().union(sq1,sq2).iterate();
        try{
            assertTrue(iterator.hasNext());   
            assertTrue(iterator.next() != null);
            assertTrue(iterator.next() != null);
            assertFalse(iterator.hasNext());
        }finally{
            iterator.close();
        }        
    }

    @Test
    public void various() throws SQLException {
        for (String s : query().from(survey).list(survey.name.lower())){
            assertEquals(s, s.toLowerCase());
        }

        for (String s : query().from(survey).list(survey.name.append("abc"))){
            assertTrue(s.endsWith("abc"));
        }

        System.out.println(query().from(survey).list(survey.id.sqrt()));

    }

    @Test
    public void variousSingleProjections(){
        // single column
        for (String s : query().from(survey).list(survey.name)){
            assertNotNull(s);
        }

        // unique single
        String s = query().from(survey).uniqueResult(survey.name);
        assertNotNull(s);

        // constructor projection
        for (IdName idAndName : query().from(survey).list(new QIdName(survey.id, survey.name))){
            assertNotNull(idAndName);
            assertNotNull(idAndName.getId());
            assertNotNull(idAndName.getName());
        }

        // unique constructor projection
        IdName idAndName = query().from(survey).uniqueResult(new QIdName(survey.id, survey.name));
        assertNotNull(idAndName);
        assertNotNull(idAndName.getId());
        assertNotNull(idAndName.getName());

        // wildcard
        for (Object[] row : query().from(survey).list(survey.all())){
            assertNotNull(row);
            assertEquals(2, row.length);
            assertNotNull(row[0]);
            assertNotNull(row[1]);
        }

        // unique wildcard
        Object[] row = query().from(survey).uniqueResult(survey.all());
        assertNotNull(row);
        assertEquals(2, row.length);
        assertNotNull(row[0]);
        assertNotNull(row[1]);

    }

    @Test
    public void variousMultiProjections(){
        // two columns
        for (Object[] row : query().from(survey).list(survey.id, survey.name)){
            assertEquals(2, row.length);
            assertEquals(Integer.class, row[0].getClass());
            assertEquals(String.class, row[1].getClass());
        }

        // column and wildcard
//        for (Object[] row : query().from(survey).list(survey.id, survey.all())){
//            assertEquals(3, row.length);
//            assertEquals(Integer.class, row[0].getClass());
//            assertNotNull(row[1]);
//            assertNotNull(row[2]);
//        }

        // projection and wildcard
//        for (Object[] row : query().from(survey).list(new QIdName(survey.id, survey.name), survey.all())){
//            assertEquals(3, row.length);
//            assertEquals(IdName.class, row[0].getClass());
//        }

        // projection and two columns
        for (Object[] row : query().from(survey).list(new QIdName(survey.id, survey.name), survey.id, survey.name)){
            assertEquals(3, row.length);
            assertEquals(IdName.class, row[0].getClass());
            assertEquals(Integer.class, row[1].getClass());
            assertEquals(String.class, row[2].getClass());
        }

        // two columns and projection
        for (Object[] row : query().from(survey).list(survey.id, survey.name, new QIdName(survey.id, survey.name))){
            assertEquals(3, row.length);
            assertEquals(Integer.class, row[0].getClass());
            assertEquals(String.class, row[1].getClass());
            assertEquals(IdName.class, row[2].getClass());
        }
        
        // wildcard and QTuple
        for (Tuple tuple : query().from(survey).list(new QTuple((survey.all())))){
            assertEquals(tuple.get(survey.id), tuple.get(0, Integer.class));
            assertEquals(tuple.get(survey.name), tuple.get(1, String.class));
        }
            
        
    }

    @Test
    public void whereExists() throws SQLException {
        SubQueryExpression<Integer> sq1 = sq().from(employee).unique(employee.id.max());

        query().from(employee).where(sq1.exists()).count();

        query().from(employee).where(sq1.exists().not()).count();
    }

    @Test
    public void tupleProjection(){
    List<Tuple> tuples = query().from(employee).list(new QTuple(employee.firstname, employee.lastname));
    assertFalse(tuples.isEmpty());
    for (Tuple tuple : tuples){
        assertNotNull(tuple.get(employee.firstname));
        assertNotNull(tuple.get(employee.lastname));
    }
    }

    @Test
    public void customProjection(){
    List<Projection> tuples = query().from(employee).list(new QProjection(employee.firstname, employee.lastname));
    assertFalse(tuples.isEmpty());
    for (Projection tuple : tuples){
        assertNotNull(tuple.get(employee.firstname));
        assertNotNull(tuple.get(employee.lastname));
        assertNotNull(tuple.getExpr(employee.firstname));
        assertNotNull(tuple.getExpr(employee.lastname));
    }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void arrayProjection(){
    List<String[]> results = query().from(employee).list(new EArrayConstructor<String>(String[].class, employee.firstname));
    assertFalse(results.isEmpty());
    for (String[] result : results){
        assertNotNull(result[0]);
    }
    }

    @Test
    public void constructorProjection(){
    List<SimpleProjection> projections =query().from(employee).list(EConstructor.create(SimpleProjection.class, employee.firstname, employee.lastname));
    assertFalse(projections.isEmpty());
    for (SimpleProjection projection : projections){
        assertNotNull(projection);
    }
    }

    @Test
    public void params(){
        Param<String> name = new Param<String>(String.class,"name");
        assertEquals("Mike",query()
                .from(employee).where(employee.firstname.eq(name))
                .set(name, "Mike")
                .uniqueResult(employee.firstname));
    }

    @Test
    public void params_anon(){
        Param<String> name = new Param<String>(String.class);
        assertEquals("Mike",query()
                .from(employee).where(employee.firstname.eq(name))
                .set(name, "Mike")
                .uniqueResult(employee.firstname));
    }

    @Test(expected=ParamNotSetException.class)
    public void params_not_set(){
        Param<String> name = new Param<String>(String.class,"name");
        assertEquals("Mike",query()
                .from(employee).where(employee.firstname.eq(name))
                .uniqueResult(employee.firstname));
    }

    @Test
    public void complex_boolean(){
        EBoolean first = employee.firstname.eq("Mike").and(employee.lastname.eq("Smith"));
        EBoolean second = employee.firstname.eq("Joe").and(employee.lastname.eq("Divis"));
        assertEquals(2, query().from(employee).where(first.or(second)).count());

        assertEquals(0, query().from(employee).where(
            employee.firstname.eq("Mike"),
            employee.lastname.eq("Smith").or(employee.firstname.eq("Joe")),
            employee.lastname.eq("Divis")
        ).count());
    }
}