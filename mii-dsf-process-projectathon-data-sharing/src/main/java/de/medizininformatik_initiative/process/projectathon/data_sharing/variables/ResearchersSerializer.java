package de.medizininformatik_initiative.process.projectathon.data_sharing.variables;

import java.io.IOException;
import java.util.Objects;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResearchersSerializer extends PrimitiveValueSerializer<ResearchersValues.ResearchersValue>
		implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public ResearchersSerializer(ObjectMapper objectMapper)
	{
		super(ResearchersValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(ResearchersValues.ResearchersValue value, ValueFields valueFields)
	{
		Researchers researchers = value.getValue();
		try
		{
			if (researchers != null)
				valueFields.setByteArrayValue(objectMapper.writeValueAsBytes(researchers));
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public ResearchersValues.ResearchersValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return ResearchersValues.create((Researchers) untypedValue.getValue());
	}

	@Override
	public ResearchersValues.ResearchersValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			Researchers Researchers = (bytes == null || bytes.length <= 0) ? null
					: objectMapper.readValue(bytes, Researchers.class);
			return ResearchersValues.create(Researchers);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
