import React, { useState } from 'react';
import { DocumentCard } from './DocumentCard';
import { LoadingSpinner } from './LoadingSpinner';
import { useDocuments } from '../hooks/useApi';

export const DocumentFeed: React.FC = () => {
  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState('introductionDate');
  const [sortDir, setSortDir] = useState('desc');
  
  const { data, isLoading, error, isFetching } = useDocuments(page, 20, sortBy, sortDir);

  if (isLoading && page === 0) {
    return (
      <div className="flex justify-center items-center min-h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <div className="flex">
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">
              Error loading documents
            </h3>
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
            <select
              value={`${sortBy}_${sortDir}`}
              onChange={(e) => {
                const [field, direction] = e.target.value.split('_');
                setSortBy(field);
                setSortDir(direction);
                setPage(0);
              }}
              className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-sm"
            >
              <option value="introductionDate_desc">Newest First</option>
              <option value="introductionDate_asc">Oldest First</option>
              <option value="title_asc">Title A-Z</option>
              <option value="title_desc">Title Z-A</option>
            </select>
          </div>
        </div>
        
        {data && (
          <div className="mt-3 text-sm text-gray-600">
            Showing {data.numberOfElements} of {data.totalElements} documents
            {data.totalPages > 1 && ` (Page ${data.number + 1} of ${data.totalPages})`}
          </div>
        )}
      </div>

      {/* Document List */}
      <div className="space-y-4">
        {data?.content.map((document) => (
          <DocumentCard key={document.id} document={document} />
        ))}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="mt-8 flex justify-center items-center space-x-2">
          <button
            onClick={() => setPage(page - 1)}
            disabled={data.first || isFetching}
            className="btn btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Previous
          </button>
          
          <span className="text-sm text-gray-600 px-4">
            Page {data.number + 1} of {data.totalPages}
          </span>
          
          <button
            onClick={() => setPage(page + 1)}
            disabled={data.last || isFetching}
            className="btn btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Next
          </button>
        </div>
      )}

      {/* Loading indicator for pagination */}
      {isFetching && page > 0 && (
        <div className="mt-4 flex justify-center">
          <LoadingSpinner size="small" />
        </div>
      )}
    </div>
  );
};
