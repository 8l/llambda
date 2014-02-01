/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

public:
	std::uint32_t length() const
	{
		return m_length;
	}

	std::uint8_t* data() const
	{
		return m_data;
	}

public:
	static bool isInstance(const DatumCell *datum)
	{
		return datum->typeId() == CellTypeId::Bytevector;
	}

private:
	std::uint32_t m_length;
	std::uint8_t* m_data;
