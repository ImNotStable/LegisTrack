import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, act } from '@testing-library/react';
import * as React from 'react';
import { BrowserRouter } from 'react-router-dom';

import { apiService } from '../../services/api';
import { DocumentFeed } from '../DocumentFeed';
import { ToastProvider } from '../Toast';

import type { Page, DocumentSummary } from '../../types';

afterEach(() => {
	jest.restoreAllMocks();
});

const renderWithProviders = (component: React.ReactElement) => {
	const queryClient = new QueryClient({
		defaultOptions: {
			queries: {
				retry: false,
				gcTime: 0,
				staleTime: Infinity,
				refetchOnWindowFocus: false,
				refetchOnReconnect: false,
				refetchOnMount: false,
			},
			mutations: { retry: false },
		},
	});

	return render(
		<QueryClientProvider client={queryClient}>
			<BrowserRouter>
				<ToastProvider>{component}</ToastProvider>
			</BrowserRouter>
		</QueryClientProvider>
	);
};

const makePage = (pageNumber: number, pageSize: number, total: number): Page<DocumentSummary> => {
	const startId = pageNumber * pageSize + 1;
	const content: DocumentSummary[] = Array.from(
		{ length: Math.min(pageSize, total - pageNumber * pageSize) },
		(_, i) => ({
			id: startId + i,
			billId: `H.R.${startId + i}`,
			title: `Bill ${startId + i}`,
			introductionDate: '2024-01-15',
			status: 'introduced',
			industryTags: [],
			partyBreakdown: {
				democratic: 1,
				republican: 1,
				independent: 0,
				other: 0,
				total: 2,
				democraticPercentage: 50,
				republicanPercentage: 50,
			},
			hasValidAnalysis: false,
		})
	);
	const totalPages = Math.ceil(total / pageSize);
	const numberOfElements = content.length;
	return {
		content,
		pageable: {
			pageNumber,
			pageSize,
			sort: { empty: false, sorted: true, unsorted: false },
			offset: pageNumber * pageSize,
			unpaged: false,
			paged: true,
		},
		last: pageNumber + 1 >= totalPages,
		totalPages,
		totalElements: total,
		size: pageSize,
		number: pageNumber,
		sort: { empty: false, sorted: true, unsorted: false },
		first: pageNumber === 0,
		numberOfElements,
		empty: numberOfElements === 0,
	};
};

describe('DocumentFeed - infinite scrolling', () => {
	it('fetches next page when sentinel intersects (no over-fetch)', async () => {
		const pageSize = 20;
		const total = 40;
		let requestCount = 0;

		jest.spyOn(apiService as any, 'getDocuments').mockImplementation((page: any, size: any) => {
			requestCount += 1;
			return Promise.resolve(makePage(page ?? 0, size ?? pageSize, total));
		});

		// Capture the IO callback used by the component
		let ioCallback: ((entries: any[]) => void) | null = null;
		const OriginalIO = (global as any).IntersectionObserver;
		(global as any).IntersectionObserver = function (cb: any) {
			ioCallback = cb;
			return {
				observe() {
					/* observe sentinel */
				},
				unobserve() {
					/* unobserve sentinel */
				},
				disconnect() {
					/* disconnect observer */
				},
			} as any;
		} as any;

		renderWithProviders(<DocumentFeed />);

		await waitFor(() => expect(screen.getByText('Bill 1')).toBeInTheDocument());
		expect(requestCount).toBe(1);

		// Trigger intersection to load page 2
		await act(async () => {
			ioCallback?.([{ isIntersecting: true } as any]);
		});

		await waitFor(() => expect(screen.getByText('Bill 21')).toBeInTheDocument());
		expect(requestCount).toBe(2);

		// Restore IO
		(global as any).IntersectionObserver = OriginalIO;
	});

	it('backs off on server errors and does not hammer APIs', async () => {
		let requestCount = 0;
		jest.spyOn(apiService as any, 'getDocuments').mockImplementation(() => {
			requestCount += 1;
			const err: any = new Error('Too Many Requests');
			(err as any).status = 429;
			return Promise.reject(err);
		});

		renderWithProviders(<DocumentFeed />);

		await waitFor(() => expect(requestCount).toBe(1));
	});
});
