package org.sfm.jdbc.jooq;

import org.jooq.Record;
import org.sfm.reflect.Getter;

public final class EnumRecordNamedGetter<R extends Record, E extends Enum<E>> implements Getter<R, E> {

	private final int index;
	private final Class<E> enumType;
	
	public EnumRecordNamedGetter(final JooqFieldKey key, Class<E> enumType) {
		this.index = key.getIndex();
		this.enumType = enumType;
	}

	@Override
	public E get(final R target) throws Exception {
		final String o = target.getValue(index, String.class);
		return (E) Enum.valueOf(enumType, String.valueOf(o));
	}
}
