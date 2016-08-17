package org.simpleflatmapper.map.fieldmapper;

import org.simpleflatmapper.map.property.FieldMapperColumnDefinition;
import org.simpleflatmapper.map.context.MappingContextFactoryBuilder;
import org.simpleflatmapper.map.mapper.PropertyMapping;
import org.simpleflatmapper.reflect.Getter;
import org.simpleflatmapper.reflect.Instantiator;
import org.simpleflatmapper.reflect.InstantiatorDefinition;
import org.simpleflatmapper.reflect.instantiator.InstantiatorDefinitions;
import org.simpleflatmapper.reflect.getter.InstantiatorGetter;
import org.simpleflatmapper.reflect.ObjectGetterFactory;
import org.simpleflatmapper.reflect.ObjectSetterFactory;
import org.simpleflatmapper.reflect.Setter;
import org.simpleflatmapper.reflect.getter.GetterFactory;
import org.simpleflatmapper.reflect.meta.ClassMeta;
import org.simpleflatmapper.reflect.meta.PropertyMeta;
import org.simpleflatmapper.map.FieldKey;
import org.simpleflatmapper.map.FieldMapper;
import org.simpleflatmapper.map.MapperBuilderErrorHandler;
import org.simpleflatmapper.util.ErrorDoc;
import org.simpleflatmapper.util.TypeHelper;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;

public final class ConstantSourceFieldMapperFactoryImpl<S, K extends FieldKey<K>> implements ConstantSourceFieldMapperFactory<S,K> {

	private final GetterFactory<? super S, ? super K> getterFactory;

	public ConstantSourceFieldMapperFactoryImpl(GetterFactory<? super S, ? super K> getterFactory) {
		this.getterFactory = getterFactory;
	}


	@SuppressWarnings("unchecked")
	private <T, P> FieldMapper<S, T> primitiveIndexedFieldMapper(final Class<P> type, final Setter<? super T, ? super P> setter, final Getter<? super S, ? extends P> getter) {
		if (type.equals(Boolean.TYPE)) {
			return new BooleanFieldMapper<S, T>(
					ObjectGetterFactory.<S>toBooleanGetter((Getter<S, ? extends Boolean>) getter),
					ObjectSetterFactory.<T>toBooleanSetter((Setter<T, ? super Boolean>) setter));
		} else if (type.equals(Integer.TYPE)) {
			return new IntFieldMapper<S, T>(
					ObjectGetterFactory.<S>toIntGetter((Getter<S, ? extends Integer>) getter),
					ObjectSetterFactory.<T>toIntSetter((Setter<T, ? super Integer>) setter));
		} else if (type.equals(Long.TYPE)) {
			return new LongFieldMapper<S, T>(
					ObjectGetterFactory.<S>toLongGetter((Getter<S, ? extends Long>) getter),
					ObjectSetterFactory.<T>toLongSetter((Setter<T, ? super Long>) setter));
		} else if (type.equals(Float.TYPE)) {
			return new FloatFieldMapper<S, T>(
					ObjectGetterFactory.<S>toFloatGetter((Getter<S, ? extends Float>) getter),
					ObjectSetterFactory.<T>toFloatSetter((Setter<T, ? super Float>) setter));
		} else if (type.equals(Double.TYPE)) {
			return new DoubleFieldMapper<S, T>(
					ObjectGetterFactory.<S>toDoubleGetter((Getter<S, ? extends Double>) getter),
					ObjectSetterFactory.<T>toDoubleSetter((Setter<T, ? super Double>) setter));
		} else if (type.equals(Byte.TYPE)) {
			return new ByteFieldMapper<S, T>(
					ObjectGetterFactory.<S>toByteGetter((Getter<S, ? extends Byte>) getter),
					ObjectSetterFactory.<T>toByteSetter((Setter<T, ? super Byte>) setter));
		} else if (type.equals(Character.TYPE)) {
			return new CharacterFieldMapper<S, T>(
					ObjectGetterFactory.<S>toCharGetter((Getter<S, ? extends Character>) getter),
					ObjectSetterFactory.<T>toCharacterSetter((Setter<T, ? super Character>) setter));
		} else if (type.equals(Short.TYPE)) {
			return new ShortFieldMapper<S, T>(
					ObjectGetterFactory.<S>toShortGetter((Getter<S, ? extends Short>) getter),
					ObjectSetterFactory.<T>toShortSetter((Setter<T, ? super Short>) setter));
		} else {
			throw new UnsupportedOperationException("Type " + type
					+ " is not primitive");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, P> FieldMapper<S, T> newFieldMapper(PropertyMapping<T, P, K,
                FieldMapperColumnDefinition<K>> propertyMapping,
												   MappingContextFactoryBuilder contextFactoryBuilder,
												   MapperBuilderErrorHandler mappingErrorHandler) {

		final PropertyMeta<T, P> propertyMeta = propertyMapping.getPropertyMeta();
		final Type propertyType = propertyMeta.getPropertyType();
		final Setter<? super T, ? super P> setter = propertyMeta.getSetter();
		final K key = propertyMapping.getColumnKey();
		final Class<P> type = TypeHelper.toClass(propertyType);

		Getter<? super S, ? extends P> getter = getGetterFor(propertyMapping);

		if (getter == null) {
			mappingErrorHandler.accessorNotFound("Could not find getter for " + key + " type " + propertyType
					+ " See " + ErrorDoc.toUrl("CSFM_GETTER_NOT_FOUND"));
			return null;
		} else {
			if (type.isPrimitive() ) {
				return this.<T, P>primitiveIndexedFieldMapper(type, setter, getter);
			}

			return new FieldMapperImpl<S, T, P>(getter, setter);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, P> Getter<? super S, ? extends P> getGetterFor(PropertyMapping<T, P, K, FieldMapperColumnDefinition<K>> propertyMapping) {
		Type propertyType = propertyMapping.getPropertyMeta().getPropertyType();

		@SuppressWarnings("unchecked")
		Getter<? super S, ? extends P> getter = (Getter<? super S, ? extends P>) propertyMapping.getColumnDefinition().getCustomGetter();

		if (getter == null && propertyMapping.getColumnDefinition().hasCustomFactory()) {
            GetterFactory<? super S, K> cGetterFactory = (GetterFactory<? super S, K>) propertyMapping.getColumnDefinition().getCustomGetterFactory();
            getter = cGetterFactory.newGetter(propertyType, propertyMapping.getColumnKey(), propertyMapping.getColumnDefinition().properties());
        }

		if (getter == null) {
            getter = getterFactory.newGetter(propertyType, propertyMapping.getColumnKey(), propertyMapping.getColumnDefinition().properties());
        }

        // try to identify constructor that we could build from
		if (getter == null) {
			getter = lookForInstantiatorGetter(propertyMapping.getPropertyMeta().getPropertyClassMeta(), propertyMapping.getColumnKey(), propertyMapping.getColumnDefinition(), new HashSet<Type>());
		}

		return getter;
	}

	public <P> Getter<? super S, ? extends P> lookForInstantiatorGetter(ClassMeta<P> classMeta, K key, FieldMapperColumnDefinition<K> columnDefinition, Collection<Type> types) {


		InstantiatorDefinitions.CompatibilityScorer scorer = InstantiatorDefinitions.getCompatibilityScorer(key);
		InstantiatorDefinition id = InstantiatorDefinitions.lookForCompatibleOneArgument(classMeta.getInstantiatorDefinitions(),
                scorer);

		if (id != null) {
            return getGettetInstantiator(classMeta, id, key, columnDefinition, types);
        }
		return null;
	}

	private <T, P> Getter<? super S, ? extends P> getGettetInstantiator(
			ClassMeta<P> classMeta,
			InstantiatorDefinition id, K key, FieldMapperColumnDefinition<K> columnDefinition,
			Collection<Type> types) {

		Instantiator<? super T, ? extends P> instantiator =
				classMeta.getReflectionService().getInstantiatorFactory().getOneArgIdentityInstantiator(id);

		final Type sourceType = id.getParameters()[0].getGenericType();

		Getter<? super S, ? extends T> subGetter = getterFactory.newGetter(sourceType, key, columnDefinition);

		if (subGetter == null) {
			if (types.contains(sourceType)) {
				// loop circuit cutter
				return null;
			}
			types.add(sourceType);
			subGetter = lookForInstantiatorGetter(classMeta.getReflectionService().<T>getClassMeta(sourceType), key, columnDefinition, types);
		}

		if (subGetter != null) {
			return new InstantiatorGetter<T, S, P>(instantiator, subGetter);
		} else return null;
	}


}
