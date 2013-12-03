#include "binding/NumericCell.h"
#include "binding/ExactIntegerCell.h"
#include "binding/InexactRationalCell.h"
#include "binding/ProperList.h"

#include "core/fatal.h"

using namespace lliby;

extern "C"
{

std::int64_t lliby_exact(NumericCell *numeric)
{
	if (auto exactInt = datum_cast<ExactIntegerCell>(numeric))
	{
		// This is already exact
		return exactInt->value();
	}

	// This must be rational; we don't need a type check
	auto inexactRational = static_cast<InexactRationalCell*>(numeric);

	if (!inexactRational->isInteger())
	{
		_lliby_fatal("Attempted to convert non-integral inexact rational to exact value", numeric);
	}

	return static_cast<std::int64_t>(inexactRational->value());
}

double lliby_inexact(NumericCell *numeric)
{
	if (auto inexactRational = datum_cast<InexactRationalCell>(numeric))
	{
		// This is already inexact
		return inexactRational->value();
	}

	// This must be an exact int; we don't need a type check
	auto exactInt = static_cast<ExactIntegerCell*>(numeric);

	// Cast to a double
	double inexactValue = static_cast<double>(exactInt->value());

	// Make sure we have the same value now. Integers larger than 2^53 aren't guaranteed to have exact douvble
	// representations
	if (static_cast<std::int64_t>(inexactValue) != exactInt->value())
	{
		_lliby_fatal("Attempted to convert exact integer with a value that cannot be represented by an inexact rational", numeric);
	}

	return inexactValue;
}

NumericCell *lliby_add(ListElementCell *argHead)
{
	const ProperList<NumericCell> argList(argHead);

	if (!argList.isValid())
	{
		_lliby_fatal("Non-numeric passed to (+)", argHead);
	}

	std::int64_t exactSum = 0;
	double inexactSum = 0.0;
	bool resultInexact = false;

	for (auto numeric : argList)
	{
		if (auto exactInteger = datum_cast<ExactIntegerCell>(numeric))
		{
			exactSum += exactInteger->value();
		}
		else
		{
			auto inexactRational = static_cast<InexactRationalCell*>(numeric);

			inexactSum += inexactRational->value();
			resultInexact = true;
		}
	}

	if (resultInexact)
	{
		return new InexactRationalCell(exactSum + inexactSum);
	}
	else
	{
		return new ExactIntegerCell(exactSum);
	}
}

NumericCell *lliby_mul(ListElementCell *argHead)
{
	const ProperList<NumericCell> argList(argHead);

	if (!argList.isValid())
	{
		_lliby_fatal("Non-numeric passed to (*)", argHead);
	}

	std::int64_t exactProduct = 1;
	double inexactProduct = 1.0;
	bool resultInexact = false;

	for (auto numeric : argList)
	{
		if (auto exactInteger = datum_cast<ExactIntegerCell>(numeric))
		{
			exactProduct *= exactInteger->value();
		}
		else
		{
			auto inexactRational = static_cast<InexactRationalCell*>(numeric);

			inexactProduct *= inexactRational->value();
			resultInexact = true;
		}
	}

	if (resultInexact)
	{
		return new InexactRationalCell(exactProduct * inexactProduct);
	}
	else
	{
		return new ExactIntegerCell(exactProduct);
	}
}

NumericCell *lliby_sub(NumericCell *startValue, ListElementCell *argHead)
{
	const ProperList<NumericCell> argList(argHead);

	if (!argList.isValid())
	{
		_lliby_fatal("Non-numeric passed to (*)", argHead);
	}

	std::int64_t exactDifference;
	double inexactDifference;
	bool resultInexact;

	if (auto exactInteger = datum_cast<ExactIntegerCell>(startValue))
	{
		if (argList.isEmpty())
		{
			// Return the inverse
			return new ExactIntegerCell(-exactInteger->value());
		}

		exactDifference = exactInteger->value();
		inexactDifference = 0.0;
		resultInexact = false;
	}
	else
	{
		auto inexactRational = static_cast<InexactRationalCell*>(startValue);

		if (argList.isEmpty())
		{
			// Return the inverse
			return new InexactRationalCell(-inexactRational->value());
		}

		exactDifference = 0;
		inexactDifference = inexactRational->value();
		resultInexact = true;
	}
	
	for (auto numeric : argList)
	{
		if (auto exactInteger = datum_cast<ExactIntegerCell>(numeric))
		{
			exactDifference -= exactInteger->value();
		}
		else
		{
			auto inexactRational = static_cast<InexactRationalCell*>(numeric);

			inexactDifference -= inexactRational->value();
			resultInexact = true;
		}
	}
	
	if (resultInexact)
	{
		return new InexactRationalCell(exactDifference + inexactDifference);
	}
	else
	{
		return new ExactIntegerCell(exactDifference);
	}
}

double lliby_div(NumericCell *startValue, ListElementCell *argHead)
{
	const ProperList<NumericCell> argList(argHead);

	if (!argList.isValid())
	{
		_lliby_fatal("Non-numeric passed to (/)", argHead);
	}

	double currentValue;

	if (auto exactInteger = datum_cast<ExactIntegerCell>(startValue))
	{
		currentValue = exactInteger->value();
	}
	else
	{
		auto inexactRational = static_cast<InexactRationalCell*>(startValue);

		currentValue = inexactRational->value();
	}

	if (argList.isEmpty())
	{
		// Return the reciprocal
		return 1.0 / currentValue;
	}
	
	for (auto numeric : argList)
	{
		if (auto exactInteger = datum_cast<ExactIntegerCell>(numeric))
		{
			currentValue /= exactInteger->value();
		}
		else
		{
			auto inexactRational = static_cast<InexactRationalCell*>(numeric);

			currentValue /= inexactRational->value();
		}
	}
	
	return currentValue;
}

}
