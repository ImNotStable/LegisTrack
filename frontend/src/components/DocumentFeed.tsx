import React, { useEffect, useMemo, useRef, useState } from 'react';
import { DocumentCard } from './DocumentCard';
import { LoadingSpinner } from './LoadingSpinner';
import { useDocuments, useInfiniteDocuments } from '../hooks/useApi';
import { useToast } from './Toast';

export const DocumentFeed: React.FC = () => {
  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState('introductionDate');
  const [sortDir, setSortDir] = useState('desc');
  
  const { data, isLoading, error, isFetching, fetchNextPage, hasNextPage } = useInfiniteDocuments(20, sortBy, sortDir);
  const toast = useToast();

  useEffect(() => {
    if (error) {
      toast.error('Failed to load documents.');
    }
  }, [error, toast]);

  const observerRef = useRef<HTMLDivElement | null>(null);
  const items = useMemo(() => data?.pages.flatMap(p => p.content) ?? [], [data]);
  const hasMore = useMemo(() => {
    if (!data?.pages?.length) return false;
    const last = data.pages[data.pages.length - 1];
    return !last.last;
  }, [data]);

  useEffect(() => {
    const el = observerRef.current;
    if (!el) return;
    const IO = (typeof window !== 'undefined') ? (window as any).IntersectionObserver : undefined;
    if (typeof IO !== 'function') return;
    try {
      const observer: IntersectionObserver = new IO(
        (entries: IntersectionObserverEntry[]) => {
          const first = entries[0];
          if (first?.isIntersecting && hasMore && !isFetching) {
            fetchNextPage();
          }
        },
        { rootMargin: '200px 0px', threshold: 0 }
      );
      if (typeof (observer as any).observe === 'function') {
        (observer as any).observe(el);
        return () => (typeof (observer as any).unobserve === 'function' ? (observer as any).unobserve(el) : undefined);
      }
    } catch {}
  }, [hasMore, isFetching, fetchNextPage]);

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
            {(() => {
              const last = data.pages[data.pages.length - 1];
              const total = last.totalElements;
              const shown = data.pages.reduce((acc, p) => acc + p.numberOfElements, 0);
              const pageNum = last.number + 1;
              const totalPages = last.totalPages;
              return (
                <>
                  Showing {shown} of {total} documents
                  {totalPages > 1 && ` (Page ${pageNum} of ${totalPages})`}
                </>
              );
            })()}
          </div>
        )}
      </div>

      {/* Document List */}
      <div className="space-y-4">
        {items.map((document) => (
          <DocumentCard key={document.id} document={document} />
        ))}
        {/* Sentinel for infinite scroll */}
        <div ref={observerRef} />
      </div>

      {/* Loading indicator for pagination */}
      {isFetching && (
        <div className="mt-4 flex justify-center">
          <LoadingSpinner size="small" />
        </div>
      )}

      {/* No manual fallback; infinite scroll only */}
    </div>
  );
};
