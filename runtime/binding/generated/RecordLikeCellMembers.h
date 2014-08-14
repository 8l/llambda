/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

public:
	bool dataIsInline() const
	{
		return m_dataIsInline;
	}

	bool isUndefined() const
	{
		return m_isUndefined;
	}

	std::uint32_t recordClassId() const
	{
		return m_recordClassId;
	}

	void* recordData() const
	{
		return m_recordData;
	}

public:
	static bool typeIdIsTypeOrSubtype(CellTypeId typeId)
	{
		return (typeId == CellTypeId::Procedure) || (typeId == CellTypeId::Record);
	}

	static bool isInstance(const AnyCell *cell)
	{
		return typeIdIsTypeOrSubtype(cell->typeId());
	}

private:
	bool m_dataIsInline;
	bool m_isUndefined;
	std::uint32_t m_recordClassId;
	void* m_recordData;
