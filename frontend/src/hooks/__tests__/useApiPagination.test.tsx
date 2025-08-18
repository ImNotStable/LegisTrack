import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';

import { apiService } from '../../services/api';
import { useDocuments } from '../useApi';

jest.mock('../../services/api');

(apiService.getDocuments as jest.Mock).mockResolvedValue({
	content: [],
	number: 0,
	size: 20,
	totalElements: 0,
	totalPages: 0,
	numberOfElements: 0,
	first: true,
	last: true,
});

const createWrapper = () => {
	const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
	const Wrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
		<QueryClientProvider client={qc}>{children}</QueryClientProvider>
	);
	Wrapper.displayName = 'QueryClientWrapper';
	return Wrapper;
};

describe('Pagination sanitization', () => {
	beforeEach(() => {
		(apiService.getDocuments as jest.Mock).mockClear();
	});

	it('invokes API even if NaN page supplied (service clamps internally)', async () => {
		const { result } = renderHook(
			() => useDocuments(Number.NaN as any, 20, 'introductionDate', 'desc'),
			{ wrapper: createWrapper() }
		);
		await waitFor(() => expect(result.current.isSuccess || result.current.isError).toBeTruthy());
		expect(apiService.getDocuments).toHaveBeenCalledTimes(1);
	});

	it('invokes API with negative page (service clamps internally)', async () => {
		const { result } = renderHook(() => useDocuments(-5, 20, 'introductionDate', 'desc'), {
			wrapper: createWrapper(),
		});
		await waitFor(() => expect(result.current.isSuccess || result.current.isError).toBeTruthy());
		expect(apiService.getDocuments).toHaveBeenCalledTimes(1);
	});
});
