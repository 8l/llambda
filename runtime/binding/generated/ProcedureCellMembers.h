/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

public:
	void* entryPoint() const
	{
		return m_entryPoint;
	}

public:
	static bool typeIdIsTypeOrSubtype(CellTypeId typeId)
	{
		return typeId == CellTypeId::Procedure;
	}

	static bool isInstance(const AnyCell *cell)
	{
		return typeIdIsTypeOrSubtype(cell->typeId());
	}

private:
	void* m_entryPoint;
