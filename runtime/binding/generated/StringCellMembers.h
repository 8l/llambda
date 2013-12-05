/*****************************************************************
 * This file is generated by gen-types.py. Do not edit manually. *
 *****************************************************************/

public:
	static StringCell* fromDatum(DatumCell *datum)
	{
		if ((datum->typeId() == CellTypeId::String))
		{
			return reinterpret_cast<StringCell*>(datum);
		}

		return nullptr;
	}

	static const StringCell* fromDatum(const DatumCell *datum)
	{
		if ((datum->typeId() == CellTypeId::String))
		{
			return reinterpret_cast<const StringCell*>(datum);
		}

		return nullptr;
	}

	static bool isInstance(const DatumCell *datum)
	{
		return (datum->typeId() == CellTypeId::String);
	}
