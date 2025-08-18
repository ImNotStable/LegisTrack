import * as React from 'react';

import { useDocuments } from '../hooks/useApi';
import { debounce } from '../utils';

import { DocumentCard } from './DocumentCard';
import { LoadingSpinner } from './LoadingSpinner';

import type { Page, DocumentSummary } from '../types';

export const DocumentFeed: React.FC = React.memo(() => {
	const [page, setPage] = React.useState(0);
	const [sortBy, setSortBy] = React.useState('introductionDate');
	const [sortDir, setSortDir] = React.useState('desc');

	const { data, isLoading, error, isFetching } = useDocuments(page, 20, sortBy, sortDir);

	// Memoize the sort options to prevent recreation on each render
	const sortOptions = React.useMemo(
		() => [
			{ value: 'introductionDate_desc', label: 'Newest First' },
			{ value: 'introductionDate_asc', label: 'Oldest First' },
			{ value: 'title_asc', label: 'Title A-Z' },
			{ value: 'title_desc', label: 'Title Z-A' },
		],
		[]
	);

	// Memoize the pagination info to prevent recalculation
	const paginationInfo = React.useMemo(() => {
		const d = data as unknown as Page<DocumentSummary> | undefined;
		if (!d) return null;
		return {
			showingText: `Showing ${d.numberOfElements} of ${d.totalElements} documents`,
			pageText: d.totalPages > 1 ? ` (Page ${d.number + 1} of ${d.totalPages})` : '',
			showPagination: d.totalPages > 1,
		};
	}, [data]);

	// Debounced sort handler to prevent excessive API calls
	const handleSortChange = React.useMemo(
		() =>
			debounce((value: string) => {
				const [field, direction] = value.split('_');
				setSortBy(field);
				setSortDir(direction);
				setPage(0);
			}, 300),
		[]
	);

	// Memoized pagination handlers
	const handlePreviousPage = React.useCallback(() => {
		setPage((prev) => Math.max(0, prev - 1));
	}, []);

	const handleNextPage = React.useCallback(() => {
		setPage((prev) => prev + 1);
	}, []);

	if (isLoading && page === 0) {
		return (
			<div className="flex justify-center items-center min-h-64">
				<LoadingSpinner size="large" />
			</div>
		);
	}

	if (error) {
		return (
			<div className="bg-red-50 border border-red-200 rounded-md p-4" role="alert">
				<div className="flex">
					<div className="ml-3">
						<h3 className="text-sm font-medium text-red-800">Error loading documents</h3>
						<div className="mt-2 text-sm text-red-700">
							<p>Please try refreshing the page. If the problem persists, contact support.</p>
						</div>
					</div>
				</div>
			</div>
		);
	}

	return (
		<div className="max-w-4xl mx-auto">
			{/* Header and Controls */}
			<div className="mb-6 bg-white p-4 rounded-lg shadow-sm border border-gray-200">
				<div className="flex justify-between items-center">
					<h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>

					<div className="flex items-center space-x-4">
						<label htmlFor="sort-select" className="sr-only">
							Sort documents by
						</label>
						<select
							id="sort-select"
							value={`${sortBy}_${sortDir}`}
							onChange={(e) => handleSortChange(e.target.value)}
							className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-sm"
							aria-label="Sort documents"
						>
							{sortOptions.map((option) => (
								<option key={option.value} value={option.value}>
									{option.label}
								</option>
							))}
						</select>
					</div>
				</div>

				{paginationInfo && (
					<div className="mt-3 text-sm text-gray-600">
						{paginationInfo.showingText}
						{paginationInfo.pageText}
					</div>
				)}
			</div>

			{/* Document List */}
			<div className="space-y-4" role="main" aria-label="Document list">
				{(data as unknown as Page<DocumentSummary> | undefined)?.content.map((document) => (
					<DocumentCard key={document.id} document={document} />
				))}
			</div>

			{/* Pagination */}
			{paginationInfo?.showPagination && (
				<nav className="mt-8 flex justify-center items-center space-x-2" aria-label="Pagination">
					<button
						onClick={handlePreviousPage}
						disabled={(data as unknown as Page<DocumentSummary> | undefined)?.first || isFetching}
						className="btn btn-secondary disabled:opacity-50 disabled:cursor-not-allowed px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
						aria-label="Go to previous page"
					>
						Previous
					</button>

					<span className="text-sm text-gray-600 px-4" aria-live="polite">
						{(() => {
							const d = data as unknown as Page<DocumentSummary> | undefined;
							return `Page ${(d?.number ?? 0) + 1} of ${d?.totalPages ?? 1}`;
						})()}
					</span>

					<button
						onClick={handleNextPage}
						disabled={(data as unknown as Page<DocumentSummary> | undefined)?.last || isFetching}
						className="btn btn-secondary disabled:opacity-50 disabled:cursor-not-allowed px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
						aria-label="Go to next page"
					>
						Next
					</button>
				</nav>
			)}

			{/* Loading overlay for subsequent pages */}
			{isFetching && page > 0 && (
				<div className="fixed inset-0 bg-black bg-opacity-25 flex items-center justify-center z-50">
					<div className="bg-white p-4 rounded-lg shadow-lg">
						<LoadingSpinner size="medium" />
						<p className="mt-2 text-sm text-gray-600">Loading more documents...</p>
					</div>
				</div>
			)}
		</div>
	);
});

DocumentFeed.displayName = 'DocumentFeed';
