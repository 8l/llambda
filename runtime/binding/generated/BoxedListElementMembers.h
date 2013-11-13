/*****************************************************************
 * This file is generated by gen-types.py. Do not edit manually. *
 *****************************************************************/

public:
	static BoxedListElement* fromDatum(BoxedDatum *datum)
	{
		if ((datum->typeId() == BoxedTypeId::Pair) || (datum->typeId() == BoxedTypeId::EmptyList))
		{
			return reinterpret_cast<BoxedListElement*>(datum);
		}

		return nullptr;
	}

	static const BoxedListElement* fromDatum(const BoxedDatum *datum)
	{
		if ((datum->typeId() == BoxedTypeId::Pair) || (datum->typeId() == BoxedTypeId::EmptyList))
		{
			return reinterpret_cast<const BoxedListElement*>(datum);
		}

		return nullptr;
	}

	static bool isInstance(const BoxedDatum *datum)
	{
		return (datum->typeId() == BoxedTypeId::Pair) || (datum->typeId() == BoxedTypeId::EmptyList);
	}
