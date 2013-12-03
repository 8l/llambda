#ifndef _LLIBY_BINDING_PROPERLIST_H
#define _LLIBY_BINDING_PROPERLIST_H

#include <iterator>

#include "ListElementCell.h"
#include "PairCell.h"
#include "EmptyListCell.h"

namespace lliby
{

template<class T>
class ProperList
{
public:
	class ConstIterator : public std::iterator<std::forward_iterator_tag, T*>
	{
		friend class ProperList;
	public:
		T* operator*() const
		{
			// ProperList verifies all the cars are of type T in its constructor
			return reinterpret_cast<T*>(m_head->car());
		}

		bool operator==(const ConstIterator &other) const
		{
			return m_head == other.m_head;
		}
		
		bool operator!=(const ConstIterator &other) const
		{
			return m_head != other.m_head;
		}

		ConstIterator& operator++()
		{
			m_head = static_cast<const PairCell*>(m_head->cdr());
			return *this;
		}
		
		ConstIterator operator++(int postfix)
		{
			ConstIterator originalValue(*this);
			++(*this);
			return originalValue;
		}

	private:
		explicit ConstIterator(const ListElementCell *head) :
			m_head(static_cast<const PairCell*>(head))
		{
		}
		
		const PairCell *m_head;
	};

	explicit ProperList(const ListElementCell *head) :
		m_head(EmptyListCell::instance()),
		m_valid(false),
		m_length(0)
	{
		const DatumCell *datum = head;
		std::uint32_t length = 0;

		while(auto pair = datum_cast<PairCell>(datum))
		{
			length++;
			
			if (datum_cast<T>(pair->car()) == nullptr)
			{
				// Wrong element type
				return;
			}

			datum = pair->cdr();
		}

		if (datum != EmptyListCell::instance())
		{
			// Not a proper list
			return;
		}

		m_head = head;
		m_valid = true;
		m_length = length;
	}

	bool isValid() const
	{
		return m_valid;
	}

	bool isEmpty() const
	{
		return m_length == 0;
	}

	std::uint32_t length() const
	{
		return m_length;
	}

	ConstIterator begin() const
	{
		return ConstIterator(m_head);
	}

	ConstIterator end() const
	{
		return ConstIterator(EmptyListCell::instance());
	}

private:
	const ListElementCell *m_head;
	bool m_valid;
	std::uint32_t m_length;
};


}

#endif
