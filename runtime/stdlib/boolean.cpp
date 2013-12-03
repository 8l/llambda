#include "binding/ListElementCell.h"
#include "binding/BooleanCell.h"
#include "binding/ProperList.h"
#include "core/fatal.h"

extern "C"
{

using namespace lliby;

bool lliby_not(bool value)
{
	return !value;
}

bool lliby_boolean_equal(BooleanCell *value1, BooleanCell *value2, ListElementCell *argHead)
{
	if (value1 != value2)
	{
		return false;
	}
	
	ProperList<BooleanCell> properList(argHead);

	if (!properList.isValid())
	{
		// We're not supposed to abort here, just return false
		_lliby_fatal("Non-boolean passed to (boolean=?)", argHead);
		return false;
	}

	for(auto boolCell : properList)
	{
		if (boolCell != value1)
		{
			return false;
		}
	}

	return true;
}

}
