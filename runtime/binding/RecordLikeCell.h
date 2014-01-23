#ifndef _LLIBY_BINDING_RECORDLIKECELL_H
#define _LLIBY_BINDING_RECORDLIKECELL_H

#include "DatumCell.h"

#include <vector>

extern "C"
{
	struct RecordClassOffsetMap;
}

namespace lliby
{

enum class RecordLikeDataStorage
{
	Empty,
	Inline,
	OutOfLne
};

class RecordLikeCell : public DatumCell
{
#include "generated/RecordLikeCellMembers.h"
public:
	static void *allocateRecordData(size_t bytes);
	void finalize();

	// Used by the garbage collector to update any references to record data stored inline
	void** recordDataRef()
	{
		return &m_recordData;
	}

	const RecordClassOffsetMap* offsetMap() const;
	RecordLikeDataStorage dataStorage() const;
	
	void finalizeRecordLike();
	
	/**
	 * Registers a runtime-created record-like class
	 *
	 * @param  offsets  List of offsets of DatumCells inside the record-like data
	 * @return Unique class ID for the new record-like class 
	 */
	static std::uint32_t registerRuntimeRecordClass(const std::vector<size_t> &offsets);

	void setRecordData(void *newData)
	{
		m_recordData = newData;
	}

protected:
	RecordLikeCell(CellTypeId typeId, std::uint32_t recordClassId, bool dataIsInline, void *recordData) :
		DatumCell(typeId),
		m_dataIsInline(dataIsInline),
		m_recordClassId(recordClassId),
		m_recordData(recordData)
	{
	}
	
	// TypeGenerator.scala always allocates this first
	static const std::uint32_t EmptyClosureRecordClassId = 0;
};

}

#endif
