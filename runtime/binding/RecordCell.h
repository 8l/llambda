#ifndef _LLIBY_BINDING_RECORDCELL_H
#define _LLIBY_BINDING_RECORDCELL_H

#include "RecordLikeCell.h"

namespace lliby
{

class RecordCell : public RecordLikeCell
{
#include "generated/RecordCellMembers.h"
public:
	RecordCell(std::uint32_t recordClassId, bool dataIsInline, void *recordData) :
		RecordLikeCell(CellTypeId::Record, recordClassId, dataIsInline, recordData)
	{
	}
};

}

#endif

