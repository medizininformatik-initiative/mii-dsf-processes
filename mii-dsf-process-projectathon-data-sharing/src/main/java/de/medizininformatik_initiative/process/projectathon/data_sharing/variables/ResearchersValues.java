package de.medizininformatik_initiative.process.projectathon.data_sharing.variables;

import java.util.Map;

import org.camunda.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

public final class ResearchersValues
{
	public static interface ResearchersValue extends PrimitiveValue<Researchers>
	{
	}

	private static class ResearchersValueImpl extends PrimitiveTypeValueImpl<Researchers> implements ResearchersValue
	{
		private static final long serialVersionUID = 1L;

		public ResearchersValueImpl(Researchers value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class ResearchersValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private ResearchersValueTypeImpl()
		{
			super(Researchers.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new ResearchersValueImpl((Researchers) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new ResearchersValueTypeImpl();

	private ResearchersValues()
	{
	}

	public static ResearchersValue create(Researchers value)
	{
		return new ResearchersValueImpl(value, VALUE_TYPE);
	}
}
