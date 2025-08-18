import {
	formatDate,
	formatShortDate,
	timeAgo,
	getPartyColor,
	getPartyAbbreviation,
	getStatusColor,
	truncateText,
	formatNumber,
	formatPercentage,
	isValidEmail,
	isValidUrl,
	storage,
	debounce,
} from '../index';

describe('Utility Functions', () => {
	describe('Date formatting', () => {
		it('formats dates correctly', () => {
			const testDate = '2024-01-15T10:30:00Z';

			expect(formatDate(testDate, 'long')).toBe('January 15, 2024');
			expect(formatDate(testDate, 'short')).toBe('Jan 15, 2024');
			expect(formatDate(testDate, 'simple')).toBe('1/15/2024');
			expect(formatShortDate(testDate)).toBe('Jan 15, 2024');
		});

		it('handles undefined dates', () => {
			expect(formatDate(undefined)).toBe('Unknown');
			expect(formatShortDate(undefined)).toBe('Unknown');
		});

		it('calculates time ago correctly', () => {
			const now = new Date();
			const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
			const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);

			expect(timeAgo(oneHourAgo.toISOString())).toBe('1 hours ago');
			expect(timeAgo(oneDayAgo.toISOString())).toBe('1 days ago');
		});
	});

	describe('Party utilities', () => {
		it('returns correct party colors', () => {
			expect(getPartyColor('Democratic')).toBe('blue');
			expect(getPartyColor('Republican')).toBe('red');
			expect(getPartyColor('Independent')).toBe('green');
			expect(getPartyColor('Other')).toBe('gray');
		});

		it('returns correct party abbreviations', () => {
			expect(getPartyAbbreviation('Democratic')).toBe('D');
			expect(getPartyAbbreviation('Republican')).toBe('R');
			expect(getPartyAbbreviation('Independent')).toBe('I');
			expect(getPartyAbbreviation('Unknown')).toBe('U');
		});
	});

	describe('Status utilities', () => {
		it('returns correct status colors', () => {
			expect(getStatusColor('passed')).toBe('bg-green-100 text-green-800');
			expect(getStatusColor('failed')).toBe('bg-red-100 text-red-800');
			expect(getStatusColor('pending')).toBe('bg-yellow-100 text-yellow-800');
			expect(getStatusColor('introduced')).toBe('bg-blue-100 text-blue-800');
			expect(getStatusColor('unknown')).toBe('bg-gray-100 text-gray-800');
		});
	});

	describe('Text utilities', () => {
		it('truncates text correctly', () => {
			const longText = 'This is a very long text that should be truncated';
			expect(truncateText(longText, 20)).toBe('This is a very long...');
			expect(truncateText('Short text', 20)).toBe('Short text');
		});
	});

	describe('Number utilities', () => {
		it('formats numbers correctly', () => {
			expect(formatNumber(500)).toBe('500');
			expect(formatNumber(1500)).toBe('1.5K');
			expect(formatNumber(1500000)).toBe('1.5M');
		});

		it('calculates percentages correctly', () => {
			expect(formatPercentage(25, 100)).toBe('25.0%');
			expect(formatPercentage(0, 100)).toBe('0.0%');
			expect(formatPercentage(10, 0)).toBe('0%');
		});
	});

	describe('Validation utilities', () => {
		it('validates emails correctly', () => {
			expect(isValidEmail('test@example.com')).toBe(true);
			expect(isValidEmail('invalid-email')).toBe(false);
			expect(isValidEmail('test@')).toBe(false);
		});

		it('validates URLs correctly', () => {
			expect(isValidUrl('https://example.com')).toBe(true);
			expect(isValidUrl('http://example.com')).toBe(true);
			expect(isValidUrl('invalid-url')).toBe(false);
		});
	});

	describe('Storage utilities', () => {
		beforeEach(() => {
			localStorage.clear();
		});

		it('stores and retrieves data correctly', () => {
			const testData = { name: 'test', value: 123 };
			storage.set('testKey', testData);

			expect(storage.get('testKey', null)).toEqual(testData);
		});

		it('returns default value for non-existent keys', () => {
			expect(storage.get('nonExistent', 'default')).toBe('default');
		});

		it('removes data correctly', () => {
			storage.set('testKey', 'value');
			storage.remove('testKey');

			expect(storage.get('testKey', 'default')).toBe('default');
		});
	});

	describe('Debounce utility', () => {
		beforeEach(() => {
			jest.useFakeTimers();
		});

		afterEach(() => {
			jest.useRealTimers();
		});

		it('debounces function calls correctly', () => {
			const mockFn = jest.fn();
			const debouncedFn = debounce(mockFn, 100);

			debouncedFn('arg1');
			debouncedFn('arg2');
			debouncedFn('arg3');

			expect(mockFn).not.toHaveBeenCalled();

			jest.advanceTimersByTime(100);

			expect(mockFn).toHaveBeenCalledTimes(1);
			expect(mockFn).toHaveBeenCalledWith('arg3');
		});
	});
});
