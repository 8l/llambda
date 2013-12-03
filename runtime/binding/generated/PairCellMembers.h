/*****************************************************************
 * This file is generated by gen-types.py. Do not edit manually. *
 *****************************************************************/

public:
	DatumCell* car() const
	{
		return m_car;
	}

	DatumCell* cdr() const
	{
		return m_cdr;
	}

public:
	static PairCell* fromDatum(DatumCell *datum)
	{
		if ((datum->typeId() == CellTypeId::Pair))
		{
			return reinterpret_cast<PairCell*>(datum);
		}

		return nullptr;
	}

	static const PairCell* fromDatum(const DatumCell *datum)
	{
		if ((datum->typeId() == CellTypeId::Pair))
		{
			return reinterpret_cast<const PairCell*>(datum);
		}

		return nullptr;
	}

	static bool isInstance(const DatumCell *datum)
	{
		return (datum->typeId() == CellTypeId::Pair);
	}

private:
	DatumCell* m_car;
	DatumCell* m_cdr;
