package org.sfm.reflect.meta;

import org.junit.Test;
import org.sfm.beans.DbObject;
import org.sfm.beans.Foo;
import org.sfm.map.MapperBuildingException;
import org.sfm.reflect.InstantiatorDefinition;
import org.sfm.reflect.ReflectionService;
import org.sfm.reflect.TypeReference;
import org.sfm.tuples.Tuple2;
import org.sfm.tuples.Tuples;

import java.lang.reflect.Type;

import static org.junit.Assert.*;

public class TuplesClassMetaTest {

    @Test
    public void isNotLeaf() {
        assertFalse(classMeta.isLeaf());
    }

    @Test
    public void failOnNoConstructorMatchingType() {
        Type type = new TypeReference<MyTuple<String, String>>() {}.getType();

        try {
            new TupleClassMeta<MyTuple<String, String>>(type, ReflectionService.newInstance());
            fail();
        }  catch (MapperBuildingException e) {
            // expect
        }
    }

    static class MyTuple<T1, T2> {
    }
    @Test
    public void testGenerateHeadersOnDbObjectString() {
        String[] names = {"element0_id", "element0_name", "element0_email", "element0_creation_time", "element0_type_ordinal", "element0_type_name", "element1"};
        assertArrayEquals(
                names,
                ReflectionService.newInstance().getClassMeta(Tuples.typeDef(DbObject.class, String.class)).generateHeaders());
    }



    @Test
    public void testFindPropertyNoAsm() {
        Type type = new TypeReference<Tuple2<String, String>>() {}.getType();

        ClassMeta<Tuple2<String, String>> classMeta = ReflectionService.disableAsm().getClassMeta(type);

        InstantiatorDefinition instantiatorDefinition = classMeta.getInstantiatorDefinitions().get(0);

        assertEquals("element0", instantiatorDefinition.getParameters()[0].getName());
        assertEquals("element1", instantiatorDefinition.getParameters()[1].getName());
        assertEquals(2, instantiatorDefinition.getParameters().length);
    }


    ClassMeta<Tuple2<Foo, Foo>> classMeta = ReflectionService.newInstance().getClassMeta(new TypeReference<Tuple2<Foo, Foo>>() {}.getType());

    @Test
    public void testIndexStartingAtZero() {
        final PropertyFinder<Tuple2<Foo, Foo>> propertyFinder = classMeta.newPropertyFinder();

        final PropertyMeta<Tuple2<Foo, Foo>, String> t0_foo = propertyFinder.findProperty(newMatcher("t0_foo"));
        final PropertyMeta<Tuple2<Foo, Foo>, String> t0_bar = propertyFinder.findProperty(newMatcher("t0_bar"));
        final PropertyMeta<Tuple2<Foo, Foo>, String> t1_foo = propertyFinder.findProperty(newMatcher("t1_foo"));
        final PropertyMeta<Tuple2<Foo, Foo>, String> t1_bar = propertyFinder.findProperty(newMatcher("t1_bar"));

        validate(t0_foo, t0_bar, t1_foo, t1_bar);

    }

    private void validate(PropertyMeta<Tuple2<Foo, Foo>, String> t0_foo,
                          PropertyMeta<Tuple2<Foo, Foo>, String> t0_bar,
                          PropertyMeta<Tuple2<Foo, Foo>, String> t1_foo,
                          PropertyMeta<Tuple2<Foo, Foo>, String> t1_bar) {

        assertNotNull(t0_foo);
        assertIs("element0", "foo", t0_foo);
        assertNotNull(t0_bar);
        assertIs("element0", "bar", t0_bar);

        assertNotNull(t1_foo);
        assertIs("element0", "foo", t0_foo);
        assertNotNull(t1_foo);
        assertIs("element0", "bar", t0_bar);

    }

    private void assertIs(String elementName, String prop, PropertyMeta<Tuple2<Foo, Foo>, String> propertyMeta) {
        assertTrue(propertyMeta.isSubProperty());
        SubPropertyMeta<Tuple2<Foo, Foo>, Foo, String> subPropertyMeta = (SubPropertyMeta<Tuple2<Foo, Foo>, Foo, String>) propertyMeta;

        assertEquals(elementName, subPropertyMeta.getOwnerProperty().getName());
        assertEquals(prop, subPropertyMeta.getSubProperty().getName());
    }

    private PropertyNameMatcher newMatcher(String name) {
        return new DefaultPropertyNameMatcher(name, 0, false, false);
    }

    @Test
    public void testIndexStartingFlexiblePrefix() {
        final PropertyFinder<Tuple2<Foo, Foo>> propertyFinder = classMeta.newPropertyFinder();

        final PropertyMeta<Tuple2<Foo, Foo>, String> t0_foo = propertyFinder.findProperty(newMatcher("ta_foo"));
        final PropertyMeta<Tuple2<Foo, Foo>, String> t0_bar = propertyFinder.findProperty(newMatcher("ta_bar"));
        final PropertyMeta<Tuple2<Foo, Foo>, String> t1_foo = propertyFinder.findProperty(newMatcher("tb_foo"));
        final PropertyMeta<Tuple2<Foo, Foo>, String> t1_bar = propertyFinder.findProperty(newMatcher("tb_bar"));
        validate(t0_foo, t0_bar, t1_foo, t1_bar);

    }


}
