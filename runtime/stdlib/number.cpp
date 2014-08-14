#include "binding/NumberCell.h"
#include "binding/ExactIntegerCell.h"
#include "binding/FlonumCell.h"
#include "binding/ProperList.h"
#include "binding/RestArgument.h"

#include <cmath>

#include "core/error.h"

using namespace lliby;

namespace
{
	double doubleValueFor(NumberCell *value)
	{
		if (auto exactInteger = cell_cast<ExactIntegerCell>(value))
		{
			return exactInteger->value();
		}
		else
		{
			auto flonum = cell_unchecked_cast<FlonumCell>(value);
			return flonum->value();
		}
	}

	template<class ExactCompare, class InexactCompare>
	bool numericCompare(NumberCell *value1, NumberCell *value2, RestArgument<NumberCell> *argHead, ExactCompare exactCompare, InexactCompare inexactCompare)
	{
		const ProperList<NumberCell> argList(argHead);

		auto compareCells = [&] (NumberCell *number1, NumberCell *number2) -> bool
		{
			auto exactNumber1 = cell_cast<ExactIntegerCell>(number1);
			auto exactNumber2 = cell_cast<ExactIntegerCell>(number2);

			if (exactNumber1 && exactNumber2)
			{
				// Both cells are exact
				return exactCompare(exactNumber1->value(), exactNumber2->value());
			}
			else if (!exactNumber1 && !exactNumber2)
			{
				// Both cells are inexact
				auto inexactNumber1 = cell_unchecked_cast<FlonumCell>(number1);
				auto inexactNumber2 = cell_unchecked_cast<FlonumCell>(number2);

				return inexactCompare(inexactNumber1->value(), inexactNumber2->value());
			}
			else if (!exactNumber1 && exactNumber2)
			{
				auto inexactNumber1 = cell_unchecked_cast<FlonumCell>(number1);

				// Try to convert to exact
				auto inexactNumber1AsExact = static_cast<std::int64_t>(inexactNumber1->value());
				if (inexactNumber1->value() == inexactNumber1AsExact)
				{
					// Compare as exact
					return exactCompare(inexactNumber1AsExact, exactNumber2->value());
				}

				// Compare as inexact
				return inexactCompare(inexactNumber1->value(), exactNumber2->value());
			}
			else // if (exactNumber1 && !exactNumber2)
			{
				auto inexactNumber2 = cell_unchecked_cast<FlonumCell>(number2);
				
				// Try to convert to exact
				auto inexactNumber2AsExact = static_cast<std::int64_t>(inexactNumber2->value());
				
				if (inexactNumber2->value() == inexactNumber2AsExact)
				{
					// Compare as exact
					return exactCompare(exactNumber1->value(), inexactNumber2AsExact);
				}

				// Compare as inexact
				return inexactCompare(exactNumber1->value(), inexactNumber2->value());
			}
		};

		if (!compareCells(value1, value2))
		{
			return false;
		}

		NumberCell *prevValue = value2;

		for(auto argListValue : argList)
		{
			if (!compareCells(prevValue, argListValue))
			{
				return false;
			}

			prevValue = argListValue;
		}

		return true;
	}
}

extern "C"
{

std::int64_t lliby_exact(World &world, NumberCell *numeric)
{
	if (auto exactInt = cell_cast<ExactIntegerCell>(numeric))
	{
		// This is already exact
		return exactInt->value();
	}

	// This must be rational; we don't need a type check
	auto flonum = cell_unchecked_cast<FlonumCell>(numeric);

	if (!flonum->isInteger())
	{
		signalError(world, "Attempted to convert non-integral inexact rational to exact value", {numeric});
	}

	return static_cast<std::int64_t>(flonum->value());
}

double lliby_inexact(World &world, NumberCell *numeric)
{
	if (auto flonum = cell_cast<FlonumCell>(numeric))
	{
		// This is already inexact
		return flonum->value();
	}

	// This must be an exact int; we don't need a type check
	auto exactInt = cell_unchecked_cast<ExactIntegerCell>(numeric);

	// Cast to a double
	double inexactValue = static_cast<double>(exactInt->value());

	// Make sure we have the same value now. Integers larger than 2^53 aren't guaranteed to have exact douvble
	// representations
	if (static_cast<std::int64_t>(inexactValue) != exactInt->value())
	{
		signalError(world, "Attempted to convert exact integer with a value that cannot be represented by an inexact rational", {numeric});
	}

	return inexactValue;
}

NumberCell *lliby_add(World &world, RestArgument<NumberCell> *argHead)
{
	const ProperList<NumberCell> argList(argHead);

	std::int64_t exactSum = 0;
	double inexactSum = 0.0;
	bool resultInexact = false;

	for (auto numeric : argList)
	{
		if (auto exactInteger = cell_cast<ExactIntegerCell>(numeric))
		{
			exactSum += exactInteger->value();
		}
		else
		{
			auto flonum = cell_unchecked_cast<FlonumCell>(numeric);

			inexactSum += flonum->value();
			resultInexact = true;
		}
	}

	if (resultInexact)
	{
		return FlonumCell::fromValue(world, exactSum + inexactSum);
	}
	else
	{
		return ExactIntegerCell::fromValue(world, exactSum);
	}
}

NumberCell *lliby_mul(World &world, RestArgument<NumberCell> *argHead)
{
	const ProperList<NumberCell> argList(argHead);

	std::int64_t exactProduct = 1;
	double inexactProduct = 1.0;
	bool resultInexact = false;

	for (auto numeric : argList)
	{
		if (auto exactInteger = cell_cast<ExactIntegerCell>(numeric))
		{
			exactProduct *= exactInteger->value();
		}
		else
		{
			auto flonum = cell_unchecked_cast<FlonumCell>(numeric);

			inexactProduct *= flonum->value();
			resultInexact = true;
		}
	}

	if (resultInexact)
	{
		return FlonumCell::fromValue(world, exactProduct * inexactProduct);
	}
	else
	{
		return ExactIntegerCell::fromValue(world, exactProduct);
	}
}

NumberCell *lliby_sub(World &world, NumberCell *startValue, RestArgument<NumberCell> *argHead)
{
	const ProperList<NumberCell> argList(argHead);

	std::int64_t exactDifference;
	double inexactDifference;
	bool resultInexact;

	if (auto exactInteger = cell_cast<ExactIntegerCell>(startValue))
	{
		if (argList.isEmpty())
		{
			// Return the inverse
			return ExactIntegerCell::fromValue(world, -exactInteger->value());
		}

		exactDifference = exactInteger->value();
		inexactDifference = 0.0;
		resultInexact = false;
	}
	else
	{
		auto flonum = cell_unchecked_cast<FlonumCell>(startValue);

		if (argList.isEmpty())
		{
			// Return the inverse
			return FlonumCell::fromValue(world, -flonum->value());
		}

		exactDifference = 0;
		inexactDifference = flonum->value();
		resultInexact = true;
	}
	
	for (auto numeric : argList)
	{
		if (auto exactInteger = cell_cast<ExactIntegerCell>(numeric))
		{
			exactDifference -= exactInteger->value();
		}
		else
		{
			auto flonum = cell_unchecked_cast<FlonumCell>(numeric);

			inexactDifference -= flonum->value();
			resultInexact = true;
		}
	}
	
	if (resultInexact)
	{
		return FlonumCell::fromValue(world, exactDifference + inexactDifference);
	}
	else
	{
		return ExactIntegerCell::fromValue(world, exactDifference);
	}
}

double lliby_div(World &world, NumberCell *startValue, RestArgument<NumberCell> *argHead)
{
	const ProperList<NumberCell> argList(argHead);

	double currentValue = doubleValueFor(startValue);

	if (argList.isEmpty())
	{
		// Return the reciprocal
		return 1.0 / currentValue;
	}
	
	for (auto numeric : argList)
	{
		currentValue /= doubleValueFor(numeric);
	}
	
	return currentValue;
}

bool lliby_is_finite(NumberCell *value)
{
	if (auto flonum = cell_cast<FlonumCell>(value))
	{
		return std::isfinite(flonum->value());
	}
	else
	{
		// Exact integers must be finite
		return true;
	}
}

bool lliby_is_infinite(NumberCell *value)
{
	if (auto flonum = cell_cast<FlonumCell>(value))
	{
		return std::isinf(flonum->value());
	}
	else
	{
		// Exact integers cannot be infinite
		return false;
	}
}

bool lliby_is_odd(std::int64_t value)
{
	// Since C++11 the remainder of a negative number mod a positive is negative
	// Before it was implementation-defined
	return (value % 2) != 0;
}

bool lliby_is_even(std::int64_t value)
{
	return (value % 2) == 0;
}

bool lliby_numeric_equal(NumberCell *value1, NumberCell *value2, RestArgument<NumberCell> *argHead)
{
	return numericCompare(value1, value2, argHead, 
			[] (std::int64_t value1, int64_t value2) { return value1 == value2; },
			[] (double value1, double value2) { return value1 == value2; });
}

bool lliby_numeric_lt(NumberCell *value1, NumberCell *value2, RestArgument<NumberCell> *argHead)
{
	return numericCompare(value1, value2, argHead, 
			[] (std::int64_t value1, int64_t value2) { return value1 < value2; },
			[] (double value1, double value2) { return value1 < value2; });
}

bool lliby_numeric_gt(NumberCell *value1, NumberCell *value2, RestArgument<NumberCell> *argHead)
{
	return numericCompare(value1, value2, argHead, 
			[] (std::int64_t value1, int64_t value2) { return value1 > value2; },
			[] (double value1, double value2) { return value1 > value2; });
}

bool lliby_numeric_lte(NumberCell *value1, NumberCell *value2, RestArgument<NumberCell> *argHead)
{
	return numericCompare(value1, value2, argHead, 
			[] (std::int64_t value1, int64_t value2) { return value1 <= value2; },
			[] (double value1, double value2) { return value1 <= value2; });
}

bool lliby_numeric_gte(NumberCell *value1, NumberCell *value2, RestArgument<NumberCell> *argHead)
{
	return numericCompare(value1, value2, argHead, 
			[] (std::int64_t value1, int64_t value2) { return value1 >= value2; },
			[] (double value1, double value2) { return value1 >= value2; });
}

}
