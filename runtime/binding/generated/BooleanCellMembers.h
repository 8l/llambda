/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

public:
	bool value() const
	{
		return m_value;
	}

public:
	static bool isInstance(const DatumCell *datum)
	{
		return datum->typeId() == CellTypeId::Boolean;
	}

private:
	bool m_value;
