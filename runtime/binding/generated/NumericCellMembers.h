/*****************************************************************
 * This file is generated by gen-types.py. Do not edit manually. *
 *****************************************************************/

public:
	static NumericCell* fromDatum(DatumCell *datum)
	{
		if ((datum->typeId() == CellTypeId::ExactInteger) || (datum->typeId() == CellTypeId::InexactRational))
		{
			return reinterpret_cast<NumericCell*>(datum);
		}

		return nullptr;
	}

	static const NumericCell* fromDatum(const DatumCell *datum)
	{
		if ((datum->typeId() == CellTypeId::ExactInteger) || (datum->typeId() == CellTypeId::InexactRational))
		{
			return reinterpret_cast<const NumericCell*>(datum);
		}

		return nullptr;
	}

	static bool isInstance(const DatumCell *datum)
	{
		return (datum->typeId() == CellTypeId::ExactInteger) || (datum->typeId() == CellTypeId::InexactRational);
	}

