/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

public:
	void* extraData() const
	{
		return m_extraData;
	}

public:
	static bool isInstance(const DatumCell *datum)
	{
		return datum->typeId() == CellTypeId::Record;
	}

private:
	void* m_extraData;
